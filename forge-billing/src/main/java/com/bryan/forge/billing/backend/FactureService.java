package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.CreateFactureLine;
import com.bryan.forge.billing.backend.dto.CreateFactureRequest;
import com.bryan.forge.billing.backend.dto.FactureDto;
import com.bryan.forge.billing.backend.dto.FactureLineDto;
import com.bryan.forge.billing.datamodel.Facture;
import com.bryan.forge.billing.datamodel.FactureLine;
import com.bryan.forge.billing.datamodel.FactureStatus;
import com.bryan.forge.billing.datarepository.FactureLineRepository;
import com.bryan.forge.billing.datarepository.FactureRepository;
import com.bryan.forge.billing.datarepository.SessionRepository;
import com.bryan.forge.billing.event.FactureValidatedEvent;
import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datamodel.RecipeComponent;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.catalog.datarepository.RecipeComponentRepository;
import com.bryan.forge.core.backend.ForbiddenException;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.ledger.backend.LedgerService;
import com.bryan.forge.ledger.datamodel.Account;
import com.bryan.forge.ledger.datamodel.MovementType;
import com.bryan.forge.ledger.datarepository.AccountRepository;
import com.bryan.forge.valuation.datamodel.Product;
import com.bryan.forge.valuation.datarepository.ProductRepository;
import io.micronaut.context.event.ApplicationEventPublisher;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class FactureService {

    private final FactureRepository factureRepo;
    private final FactureLineRepository lineRepo;
    private final SessionRepository sessionRepo;
    private final ItemRepository itemRepo;
    private final RecipeComponentRepository recipeRepo;
    private final ProductRepository productRepo;
    private final CostingService costingService;
    private final TaxRateService taxRateService;
    private final LedgerService ledgerService;
    private final AccountRepository accountRepo;
    private final BusinessRepository businessRepo;
    private final BusinessAccessService access;
    private final ApplicationEventPublisher<Object> events;

    public FactureService(FactureRepository factureRepo, FactureLineRepository lineRepo, SessionRepository sessionRepo,
                          ItemRepository itemRepo, RecipeComponentRepository recipeRepo, ProductRepository productRepo,
                          CostingService costingService, TaxRateService taxRateService, LedgerService ledgerService,
                          AccountRepository accountRepo, BusinessRepository businessRepo, BusinessAccessService access,
                          ApplicationEventPublisher<Object> events) {
        this.factureRepo = factureRepo;
        this.lineRepo = lineRepo;
        this.sessionRepo = sessionRepo;
        this.itemRepo = itemRepo;
        this.recipeRepo = recipeRepo;
        this.productRepo = productRepo;
        this.costingService = costingService;
        this.taxRateService = taxRateService;
        this.ledgerService = ledgerService;
        this.accountRepo = accountRepo;
        this.businessRepo = businessRepo;
        this.access = access;
        this.events = events;
    }

    @Transactional
    public List<FactureDto> list(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        Map<UUID, String> names = itemNames();
        return factureRepo.findByBusinessIdOrderByNumeroDesc(businessId).stream()
                .map(f -> toDto(f, lineRepo.findByFactureId(f.getId()), names))
                .toList();
    }

    @Transactional
    public FactureDto get(User actor, UUID businessId, UUID factureId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        Facture facture = requireFacture(factureId, businessId);
        return toDto(facture, lineRepo.findByFactureId(factureId), itemNames());
    }

    /** Crée une facture BROUILLON ; prix de ligne pré-rempli depuis le prix de revente courant. */
    @Transactional
    public FactureDto create(User actor, UUID businessId, CreateFactureRequest req) {
        requireBusiness(businessId);
        access.requireOperate(actor, businessId);
        if (req.lines() == null || req.lines().isEmpty()) {
            throw new IllegalArgumentException("Une facture doit avoir au moins une ligne");
        }

        Facture facture = new Facture(businessId, factureRepo.nextNumero(), actor.getId(),
                blankToNull(req.clientName()), blankToNull(req.clientNote()));
        // Rattache au poste ouvert de l'utilisateur, s'il y en a un.
        sessionRepo.findByBusinessIdAndUserIdAndClosedAtIsNull(businessId, actor.getId())
                .ifPresent(s -> facture.setSessionId(s.getId()));
        Facture saved = factureRepo.save(facture);

        for (CreateFactureLine line : req.lines()) {
            if (line.quantity() <= 0) {
                throw new IllegalArgumentException("Quantité invalide");
            }
            itemRepo.findById(line.itemId())
                    .orElseThrow(() -> new NoSuchElementException("Item introuvable : " + line.itemId()));
            // Prix négocié surchargé, sinon valeur catalogue courante.
            BigDecimal price = line.unitPrice() != null && line.unitPrice().signum() >= 0
                    ? line.unitPrice()
                    : productRepo.findByBusinessIdAndItemIdAndValidToIsNull(businessId, line.itemId())
                            .map(Product::getPrixRevente).filter(java.util.Objects::nonNull).orElse(BigDecimal.ZERO);
            lineRepo.save(new FactureLine(saved.getId(), line.itemId(), line.quantity(), price));
        }
        return toDto(saved, lineRepo.findByFactureId(saved.getId()), itemNames());
    }

    /** Ajuste prix négocié et quantité d'une ligne (facture BROUILLON uniquement). */
    @Transactional
    public FactureDto updateLine(User actor, UUID businessId, UUID factureId, UUID lineId,
                                 BigDecimal unitPrice, int quantity) {
        requireBusiness(businessId);
        access.requireOperate(actor, businessId);
        Facture facture = requireFacture(factureId, businessId);
        requireDraft(facture);
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Prix invalide");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantité invalide");
        }
        FactureLine line = lineRepo.findById(lineId)
                .orElseThrow(() -> new NoSuchElementException("Ligne introuvable : " + lineId));
        if (!line.getFactureId().equals(factureId)) {
            throw new ForbiddenException("Cette ligne n'appartient pas à la facture");
        }
        line.setUnitPriceSnapshot(unitPrice);
        line.setQuantity(quantity);
        lineRepo.update(line);
        return toDto(facture, lineRepo.findByFactureId(factureId), itemNames());
    }

    /**
     * Valide la facture : fige prix + coûts, calcule les totaux (arrondi par excès à la
     * ligne), découpe le bénéfice selon la taxe, et génère atomiquement les mouvements
     * (marchandise OUT du compte stock, septimes IN dans le coffre). Cf. CDC §6.3.
     */
    @Transactional
    public FactureDto validate(User actor, UUID businessId, UUID factureId, boolean paid,
                               UUID stockAccountId, UUID coffreAccountId) {
        access.requireOperate(actor, businessId);
        Business business = businessRepo.findById(businessId)
                .orElseThrow(() -> new NoSuchElementException("Business introuvable : " + businessId));
        Facture facture = requireFacture(factureId, businessId);
        requireDraft(facture);

        // Comptes : ceux fournis, sinon les comptes par défaut du business (POS).
        UUID stock = stockAccountId != null ? stockAccountId : business.getDefaultStockAccountId();
        UUID coffre = coffreAccountId != null ? coffreAccountId : business.getDefaultCoffreAccountId();
        if (stock == null) {
            throw new IllegalStateException("Aucun compte stock (configure un compte par défaut)");
        }
        requireAccount(stock, businessId);
        if (paid) {
            if (coffre == null) {
                throw new IllegalStateException("Aucun coffre (configure un coffre par défaut)");
            }
            requireAccount(coffre, businessId);
        }

        List<FactureLine> lines = lineRepo.findByFactureId(factureId);
        if (lines.isEmpty()) {
            throw new IllegalStateException("Facture sans ligne");
        }

        long totalAmount = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        for (FactureLine line : lines) {
            BigDecimal unitCost = costingService.costOf(businessId, line.getItemId());
            long lineTotal = line.getUnitPriceSnapshot()
                    .multiply(BigDecimal.valueOf(line.getQuantity()))
                    .setScale(0, RoundingMode.CEILING)
                    .longValueExact();
            line.setUnitCostSnapshot(unitCost);
            line.setLineTotal(lineTotal);
            lineRepo.update(line);

            totalAmount += lineTotal;
            totalCost = totalCost.add(unitCost.multiply(BigDecimal.valueOf(line.getQuantity())));
        }

        BigDecimal totalProfit = BigDecimal.valueOf(totalAmount).subtract(totalCost);
        BigDecimal taxRate = taxRateService.currentRate(businessId);
        BigDecimal businessShare = totalProfit.multiply(taxRate);
        BigDecimal workerShare = totalProfit.subtract(businessShare);

        facture.setStatus(FactureStatus.VALIDEE);
        facture.setValidatedAt(Instant.now());
        facture.setPaid(paid);
        facture.setTotalAmount(totalAmount);
        facture.setTotalCost(totalCost);
        facture.setTotalProfit(totalProfit);
        facture.setTaxRateSnapshot(taxRate);
        facture.setBusinessShare(businessShare);
        facture.setWorkerShare(workerShare);
        factureRepo.update(facture);

        // Mouvements (atomique) : marchandise OUT toujours ; septimes IN seulement si payée.
        // Objet en stock → on sort l'objet fini. Sinon → forge à la demande : on consomme
        // les ingrédients de la recette (1 niveau, les éléments de base). La garde stock
        // négatif de applyMovement refuse la vente si un ingrédient manque (rollback).
        String ref = "Facture #" + facture.getNumero();
        for (FactureLine line : lines) {
            long inStock = ledgerService.balanceOf(stock, line.getItemId());
            if (inStock >= line.getQuantity()) {
                ledgerService.applyMovement(businessId, line.getItemId(), line.getQuantity(),
                        stock, null, MovementType.SALE, "FACTURE", factureId, ref, actor.getId());
            } else {
                List<RecipeComponent> recipe = recipeRepo.findByOutputItemId(line.getItemId());
                if (recipe.isEmpty()) {
                    throw new IllegalStateException("Stock insuffisant et aucune recette pour fabriquer l'objet vendu");
                }
                for (RecipeComponent rc : recipe) {
                    ledgerService.applyMovement(businessId, rc.getComponentItem().getId(),
                            rc.getQuantity() * line.getQuantity(), stock, null,
                            MovementType.CONSUMPTION, "FACTURE", factureId, ref + " (forge)", actor.getId());
                }
            }
        }
        if (paid && totalAmount > 0) {
            ledgerService.applyMovement(businessId, septimeId(), (int) totalAmount,
                    null, coffre, MovementType.SALE, "FACTURE", factureId, ref, actor.getId());
        }

        events.publishEvent(new FactureValidatedEvent(actor.getUsername(), actor.isWebhooksEnabled(),
                facture.getNumero(), totalAmount, totalCost, totalProfit, businessShare, workerShare));

        return toDto(facture, lines, itemNames());
    }

    /** Encaisse une facture à crédit déjà validée : septims → coffre, marque payée. */
    @Transactional
    public FactureDto markPaid(User actor, UUID businessId, UUID factureId, UUID coffreAccountId) {
        access.requireOperate(actor, businessId);
        Business business = businessRepo.findById(businessId)
                .orElseThrow(() -> new NoSuchElementException("Business introuvable : " + businessId));
        Facture facture = requireFacture(factureId, businessId);
        if (facture.getStatus() != FactureStatus.VALIDEE || facture.isPaid()) {
            throw new IllegalStateException("Facture non encaissable");
        }
        UUID coffre = coffreAccountId != null ? coffreAccountId : business.getDefaultCoffreAccountId();
        if (coffre == null) {
            throw new IllegalStateException("Aucun coffre (configure un coffre par défaut)");
        }
        requireAccount(coffre, businessId);
        if (facture.getTotalAmount() > 0) {
            ledgerService.applyMovement(businessId, septimeId(), (int) facture.getTotalAmount(),
                    null, coffre, MovementType.SALE, "FACTURE", factureId, "Encaissement #" + facture.getNumero(), actor.getId());
        }
        facture.setPaid(true);
        factureRepo.update(facture);
        return toDto(facture, lineRepo.findByFactureId(factureId), itemNames());
    }

    private UUID septimeId() {
        return itemRepo.findFirstBySystemTrue()
                .orElseThrow(() -> new IllegalStateException("Item septime introuvable")).getId();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void requireBusiness(UUID businessId) {
        if (businessRepo.findById(businessId).isEmpty()) {
            throw new NoSuchElementException("Business introuvable : " + businessId);
        }
    }

    private Facture requireFacture(UUID factureId, UUID businessId) {
        Facture facture = factureRepo.findById(factureId)
                .orElseThrow(() -> new NoSuchElementException("Facture introuvable : " + factureId));
        if (!facture.getBusinessId().equals(businessId)) {
            throw new ForbiddenException("Cette facture n'appartient pas au business");
        }
        return facture;
    }

    private void requireDraft(Facture facture) {
        if (facture.getStatus() != FactureStatus.BROUILLON) {
            throw new IllegalStateException("Facture déjà validée");
        }
    }

    private void requireAccount(UUID accountId, UUID businessId) {
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Compte introuvable : " + accountId));
        if (!account.getBusinessId().equals(businessId)) {
            throw new ForbiddenException("Ce compte n'appartient pas au business");
        }
    }

    private Map<UUID, String> itemNames() {
        return itemRepo.findAll().stream().collect(Collectors.toMap(Item::getId, Item::getName));
    }

    private FactureDto toDto(Facture f, List<FactureLine> lines, Map<UUID, String> itemNames) {
        List<FactureLineDto> lineDtos = lines.stream()
                .map(l -> new FactureLineDto(l.getId(), l.getItemId(), itemNames.getOrDefault(l.getItemId(), "?"),
                        l.getQuantity(), l.getUnitPriceSnapshot(), l.getUnitCostSnapshot(), l.getLineTotal()))
                .toList();
        return new FactureDto(f.getId(), f.getNumero(), f.getStatus(), f.isPaid(), f.getClientName(),
                f.getTotalAmount(), f.getTotalCost(), f.getTotalProfit(), f.getTaxRateSnapshot(),
                f.getBusinessShare(), f.getWorkerShare(), f.getClientNote(), f.getInternalNote(),
                f.getCreatedAt(), f.getValidatedAt(), lineDtos);
    }
}
