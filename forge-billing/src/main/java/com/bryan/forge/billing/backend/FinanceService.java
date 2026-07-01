package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.CreateExpenseRequest;
import com.bryan.forge.billing.backend.dto.ExpenseDto;
import com.bryan.forge.billing.backend.dto.FinanceSummaryDto;
import com.bryan.forge.billing.backend.dto.ForgeronOwedDto;
import com.bryan.forge.billing.backend.dto.PayRequest;
import com.bryan.forge.billing.backend.dto.PayoutDto;
import com.bryan.forge.billing.datamodel.Expense;
import com.bryan.forge.billing.datamodel.Facture;
import com.bryan.forge.billing.datamodel.FactureStatus;
import com.bryan.forge.billing.datamodel.Payout;
import com.bryan.forge.billing.datarepository.ExpenseRepository;
import com.bryan.forge.billing.datarepository.FactureRepository;
import com.bryan.forge.billing.datarepository.PayoutRepository;
import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.core.datarepository.UserRepository;
import com.bryan.forge.ledger.backend.LedgerService;
import com.bryan.forge.ledger.datamodel.MovementType;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

/**
 * Finance d'un business : versement des parts forgeron (paie), dépenses et compte de résultat.
 * La part d'un forgeron = Σ workerShare de ses factures VALIDEE ; le solde dû = gagné − déjà versé.
 */
@Singleton
public class FinanceService {

    private final FactureRepository factureRepo;
    private final PayoutRepository payoutRepo;
    private final ExpenseRepository expenseRepo;
    private final ItemRepository itemRepo;
    private final BusinessRepository businessRepo;
    private final UserRepository userRepo;
    private final BusinessAccessService access;
    private final LedgerService ledgerService;
    private final TaxRateService taxRateService;

    public FinanceService(FactureRepository factureRepo, PayoutRepository payoutRepo,
                          ExpenseRepository expenseRepo, ItemRepository itemRepo,
                          BusinessRepository businessRepo, UserRepository userRepo,
                          BusinessAccessService access, LedgerService ledgerService,
                          TaxRateService taxRateService) {
        this.factureRepo = factureRepo;
        this.payoutRepo = payoutRepo;
        this.expenseRepo = expenseRepo;
        this.itemRepo = itemRepo;
        this.businessRepo = businessRepo;
        this.userRepo = userRepo;
        this.access = access;
        this.ledgerService = ledgerService;
        this.taxRateService = taxRateService;
    }

    @Transactional
    public List<ForgeronOwedDto> owed(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        Map<UUID, BigDecimal> earned = earnedByForgeron(businessId);
        Map<UUID, Long> paid = paidByForgeron(businessId);
        Set<UUID> users = new HashSet<>(earned.keySet());
        users.addAll(paid.keySet());
        return users.stream().map(uid -> {
            long e = round(earned.getOrDefault(uid, BigDecimal.ZERO));
            long p = paid.getOrDefault(uid, 0L);
            return new ForgeronOwedDto(uid, displayName(uid), e, p, e - p);
        }).sorted(Comparator.comparingLong(ForgeronOwedDto::owed).reversed()).toList();
    }

    /** Verse une part à un forgeron : septimes sortis du coffre par défaut. Réservé ADMIN. */
    @Transactional
    public PayoutDto pay(User actor, UUID businessId, PayRequest req) {
        requireBusiness(businessId);
        access.requireAdmin(actor, businessId);
        if (req.amount() <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }
        long e = round(earnedByForgeron(businessId).getOrDefault(req.forgeronUserId(), BigDecimal.ZERO));
        long alreadyPaid = paidByForgeron(businessId).getOrDefault(req.forgeronUserId(), 0L);
        if (req.amount() > e - alreadyPaid) {
            throw new IllegalArgumentException("Montant supérieur au solde dû (" + (e - alreadyPaid) + ")");
        }
        UUID coffre = requireCoffre(businessId);
        String note = req.note() == null || req.note().isBlank() ? null : req.note().trim();
        ledgerService.applyMovement(businessId, septimeId(), (int) req.amount(), coffre, null,
                MovementType.WITHDRAWAL, "PAIE", null, "Paie " + displayName(req.forgeronUserId()), actor.getId());
        Payout saved = payoutRepo.save(new Payout(businessId, req.forgeronUserId(), req.amount(), note, actor.getId()));
        return toDto(saved);
    }

    @Transactional
    public List<PayoutDto> payouts(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        return payoutRepo.findByBusinessIdOrderByCreatedAtDesc(businessId).stream().map(this::toDto).toList();
    }

    /** Enregistre une dépense : septimes sortis du coffre par défaut. Réservé ADMIN. */
    @Transactional
    public ExpenseDto addExpense(User actor, UUID businessId, CreateExpenseRequest req) {
        requireBusiness(businessId);
        access.requireAdmin(actor, businessId);
        if (req.label() == null || req.label().isBlank()) {
            throw new IllegalArgumentException("Libellé requis");
        }
        if (req.amount() <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }
        UUID coffre = requireCoffre(businessId);
        String category = req.category() == null || req.category().isBlank() ? null : req.category().trim();
        ledgerService.applyMovement(businessId, septimeId(), (int) req.amount(), coffre, null,
                MovementType.WITHDRAWAL, "DEPENSE", null, "Dépense : " + req.label().trim(), actor.getId());
        Expense saved = expenseRepo.save(new Expense(businessId, req.label().trim(), req.amount(), category, actor.getId()));
        return new ExpenseDto(saved.getId(), saved.getLabel(), saved.getAmount(), saved.getCategory(), saved.getCreatedAt());
    }

    @Transactional
    public List<ExpenseDto> expenses(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        return expenseRepo.findByBusinessIdOrderByCreatedAtDesc(businessId).stream()
                .map(x -> new ExpenseDto(x.getId(), x.getLabel(), x.getAmount(), x.getCategory(), x.getCreatedAt()))
                .toList();
    }

    @Transactional
    public FinanceSummaryDto summary(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        List<Facture> validated = validatedFactures(businessId);
        long ca = validated.stream().filter(Facture::isPaid).mapToLong(Facture::getTotalAmount).sum();
        long cout = round(sum(validated, Facture::getTotalCost));
        long benefice = round(sum(validated, Facture::getTotalProfit));
        long partBusiness = round(sum(validated, Facture::getBusinessShare));
        long partForgerons = round(sum(validated, Facture::getWorkerShare));
        long paie = payoutRepo.findByBusinessIdOrderByCreatedAtDesc(businessId).stream().mapToLong(Payout::getAmount).sum();
        long depenses = expenseRepo.findByBusinessIdOrderByCreatedAtDesc(businessId).stream().mapToLong(Expense::getAmount).sum();
        return new FinanceSummaryDto(ca, cout, benefice, partBusiness, partForgerons, paie, depenses, partBusiness - depenses);
    }

    // ── Taxe de la ville ──────────────────────────────────────────────────────

    private static final String CITY_TAX = "Taxe ville";

    /**
     * Taxe ville due = forfait hebdo × semaines écoulées depuis le réglage courant
     * + taux × Σ(CA − part forgeron), cumulatif − déjà reversé (catégorie « Taxe ville »).
     * À reverser ~1×/semaine.
     */
    @Transactional
    public long cityTaxDue(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        return cityTaxDueInternal(businessId);
    }

    /** Variante sans contrôle d'accès (rappel hebdo planifié). */
    @Transactional
    public long cityTaxDueInternal(UUID businessId) {
        // Part variable : % du CA après paie des forgerons = Σ(CA − part forgeron).
        BigDecimal caApresForgerons = validatedFactures(businessId).stream()
                .map(f -> BigDecimal.valueOf(f.getTotalAmount()).subtract(f.getWorkerShare()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long variable = round(caApresForgerons.multiply(taxRateService.currentCityRate(businessId)));
        // Forfait : hebdo × semaines écoulées depuis l'entrée en vigueur du réglage courant
        // (pas depuis la création du business — sinon un forfait réglé tard serait rétroactif).
        long fixed = taxRateService.currentEntity(businessId)
                .map(t -> t.getCityFixed() * Math.max(0, Duration.between(t.getValidFrom(), Instant.now()).toDays() / 7))
                .orElse(0L);
        long reversed = expenseRepo.findByBusinessIdOrderByCreatedAtDesc(businessId).stream()
                .filter(e -> CITY_TAX.equals(e.getCategory())).mapToLong(Expense::getAmount).sum();
        return Math.max(0, fixed + variable - reversed);
    }

    /** Reverse la taxe de la ville : dépense historisée (septimes sortis du coffre). ADMIN. */
    @Transactional
    public ExpenseDto payCityTax(User actor, UUID businessId, long amount) {
        return addExpense(actor, businessId, new CreateExpenseRequest("Taxe de la ville", amount, CITY_TAX));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private List<Facture> validatedFactures(UUID businessId) {
        return factureRepo.findByBusinessIdOrderByNumeroDesc(businessId).stream()
                .filter(f -> f.getStatus() == FactureStatus.VALIDEE)
                .toList();
    }

    private Map<UUID, BigDecimal> earnedByForgeron(UUID businessId) {
        Map<UUID, BigDecimal> m = new HashMap<>();
        for (Facture f : validatedFactures(businessId)) {
            m.merge(f.getCreatedBy(), f.getWorkerShare(), BigDecimal::add);
        }
        return m;
    }

    private Map<UUID, Long> paidByForgeron(UUID businessId) {
        Map<UUID, Long> m = new HashMap<>();
        for (Payout p : payoutRepo.findByBusinessIdOrderByCreatedAtDesc(businessId)) {
            m.merge(p.getForgeronUserId(), p.getAmount(), Long::sum);
        }
        return m;
    }

    private static BigDecimal sum(List<Facture> factures, java.util.function.Function<Facture, BigDecimal> f) {
        return factures.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static long round(BigDecimal b) {
        return b.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private UUID septimeId() {
        return itemRepo.findFirstBySystemTrue()
                .orElseThrow(() -> new IllegalStateException("Item septime introuvable")).getId();
    }

    private UUID requireCoffre(UUID businessId) {
        Business b = businessRepo.findById(businessId)
                .orElseThrow(() -> new NoSuchElementException("Business introuvable : " + businessId));
        if (b.getDefaultCoffreAccountId() == null) {
            throw new IllegalStateException("Aucun coffre par défaut — configure-le d'abord.");
        }
        return b.getDefaultCoffreAccountId();
    }

    private void requireBusiness(UUID businessId) {
        if (businessRepo.findById(businessId).isEmpty()) {
            throw new NoSuchElementException("Business introuvable : " + businessId);
        }
    }

    private String displayName(UUID userId) {
        return userRepo.findById(userId).map(User::getDisplayName).orElse("?");
    }

    private PayoutDto toDto(Payout p) {
        return new PayoutDto(p.getId(), p.getForgeronUserId(), displayName(p.getForgeronUserId()),
                p.getAmount(), p.getNote(), p.getCreatedAt());
    }
}
