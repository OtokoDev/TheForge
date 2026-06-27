package com.bryan.forge.treasury.backend;

import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.core.backend.AuditService;
import com.bryan.forge.core.backend.ForbiddenException;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.core.datarepository.UserRepository;
import com.bryan.forge.ledger.backend.LedgerService;
import com.bryan.forge.ledger.datamodel.Account;
import com.bryan.forge.ledger.datamodel.MovementType;
import com.bryan.forge.ledger.datarepository.AccountRepository;
import com.bryan.forge.treasury.backend.dto.CreanceEntryDto;
import com.bryan.forge.treasury.backend.dto.CreanceFarmerDto;
import com.bryan.forge.treasury.backend.dto.DepositLine;
import com.bryan.forge.treasury.datamodel.CreanceEntry;
import com.bryan.forge.treasury.datamodel.CreanceType;
import com.bryan.forge.treasury.datarepository.CreanceEntryRepository;
import com.bryan.forge.valuation.datamodel.Product;
import com.bryan.forge.valuation.datarepository.ProductRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Singleton
public class CreanceService {

    private final CreanceEntryRepository repo;
    private final ItemRepository itemRepo;
    private final ProductRepository productRepo;
    private final LedgerService ledgerService;
    private final AccountRepository accountRepo;
    private final BusinessRepository businessRepo;
    private final UserRepository userRepo;
    private final BusinessAccessService access;
    private final AuditService audit;

    public CreanceService(CreanceEntryRepository repo, ItemRepository itemRepo, ProductRepository productRepo,
                          LedgerService ledgerService, AccountRepository accountRepo, BusinessRepository businessRepo,
                          UserRepository userRepo, BusinessAccessService access, AuditService audit) {
        this.repo = repo;
        this.itemRepo = itemRepo;
        this.productRepo = productRepo;
        this.ledgerService = ledgerService;
        this.accountRepo = accountRepo;
        this.businessRepo = businessRepo;
        this.userRepo = userRepo;
        this.access = access;
        this.audit = audit;
    }

    /**
     * Dépôt d'un farmeur : les items entrent dans le compte stock (mouvements) et la
     * créance CREDIT est valorisée = Σ(qté × valeur courante), arrondi par excès.
     */
    @Transactional
    public CreanceFarmerDto deposit(User actor, UUID businessId, UUID farmerUserId, List<DepositLine> lines,
                                    UUID stockAccountId, String reference) {
        requireBusiness(businessId);
        access.requireOperate(actor, businessId);
        User farmer = requireUser(farmerUserId);
        requireAccount(stockAccountId, businessId);
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Aucun item déposé");
        }

        BigDecimal value = BigDecimal.ZERO;
        for (DepositLine line : lines) {
            if (line.quantity() <= 0) {
                throw new IllegalArgumentException("Quantité invalide");
            }
            Item item = itemRepo.findById(line.itemId())
                    .orElseThrow(() -> new NoSuchElementException("Item introuvable : " + line.itemId()));
            // Dette envers le fermier = valeur (coût) des matières déposées.
            BigDecimal unit = item.isSystem()
                    ? BigDecimal.ONE
                    : productRepo.findByBusinessIdAndItemIdAndValidToIsNull(businessId, line.itemId())
                            .map(Product::getValeur).filter(java.util.Objects::nonNull).orElse(BigDecimal.ZERO);
            value = value.add(unit.multiply(BigDecimal.valueOf(line.quantity())));
            ledgerService.applyMovement(businessId, line.itemId(), line.quantity(), null, stockAccountId,
                    MovementType.DEPOSIT, "CREANCE", null, "Dépôt " + farmer.getUsername(), actor.getId());
        }

        long amount = value.setScale(0, RoundingMode.CEILING).longValueExact();
        if (amount > 0) {
            repo.save(new CreanceEntry(businessId, farmerUserId, CreanceType.CREDIT, amount, reference, actor.getId()));
        }
        audit.record(businessId, actor.getId(), "CREANCE_DEPOT",
                "Dépôt " + farmer.getUsername() + " — " + amount + " or (" + lines.size() + " ligne(s))");
        return balance(businessId, farmerUserId, farmer.getUsername());
    }

    /** Paiement d'un farmeur : septimes sortis du coffre (garde solde négatif) + entrée PAIEMENT. */
    @Transactional
    public CreanceFarmerDto pay(User actor, UUID businessId, UUID farmerUserId, long amount,
                                UUID coffreAccountId, String reference) {
        requireBusiness(businessId);
        access.requireOperate(actor, businessId);
        User farmer = requireUser(farmerUserId);
        requireAccount(coffreAccountId, businessId);
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }

        UUID septimeId = itemRepo.findFirstBySystemTrue()
                .orElseThrow(() -> new IllegalStateException("Item septime introuvable")).getId();
        ledgerService.applyMovement(businessId, septimeId, (int) amount, coffreAccountId, null,
                MovementType.WITHDRAWAL, "CREANCE", null, "Paiement " + farmer.getUsername(), actor.getId());

        repo.save(new CreanceEntry(businessId, farmerUserId, CreanceType.PAIEMENT, amount, reference, actor.getId()));
        audit.record(businessId, actor.getId(), "CREANCE_PAIEMENT",
                "Paiement " + farmer.getUsername() + " — " + amount + " or");
        return balance(businessId, farmerUserId, farmer.getUsername());
    }

    /** Soldes de tous les farmeurs ayant une créance dans le business. */
    @Transactional
    public List<CreanceFarmerDto> listFarmers(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);

        Map<UUID, long[]> agg = new HashMap<>(); // [credit, paid]
        for (CreanceEntry e : repo.findByBusinessId(businessId)) {
            long[] a = agg.computeIfAbsent(e.getFarmerUserId(), k -> new long[2]);
            if (e.getType() == CreanceType.CREDIT) a[0] += e.getAmount();
            else a[1] += e.getAmount();
        }

        Map<UUID, String> names = new HashMap<>();
        return agg.entrySet().stream()
                .map(en -> {
                    String name = names.computeIfAbsent(en.getKey(),
                            id -> userRepo.findById(id).map(User::getUsername).orElse("?"));
                    long credit = en.getValue()[0];
                    long paid = en.getValue()[1];
                    return new CreanceFarmerDto(en.getKey(), name, credit, paid, credit - paid);
                })
                .sorted(Comparator.comparingLong(CreanceFarmerDto::remaining).reversed())
                .toList();
    }

    /** Historique des écritures d'un farmeur. */
    @Transactional
    public List<CreanceEntryDto> entries(User actor, UUID businessId, UUID farmerUserId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        Map<UUID, String> names = new HashMap<>();
        return repo.findByBusinessIdAndFarmerUserIdOrderByCreatedAtDesc(businessId, farmerUserId).stream()
                .map(e -> new CreanceEntryDto(e.getType(), e.getAmount(), e.getReference(),
                        names.computeIfAbsent(e.getCreatedBy(),
                                id -> userRepo.findById(id).map(User::getUsername).orElse("?")),
                        e.getCreatedAt()))
                .toList();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private CreanceFarmerDto balance(UUID businessId, UUID farmerUserId, String username) {
        long credit = 0;
        long paid = 0;
        for (CreanceEntry e : repo.findByBusinessIdAndFarmerUserIdOrderByCreatedAtDesc(businessId, farmerUserId)) {
            if (e.getType() == CreanceType.CREDIT) credit += e.getAmount();
            else paid += e.getAmount();
        }
        return new CreanceFarmerDto(farmerUserId, username, credit, paid, credit - paid);
    }

    private void requireBusiness(UUID businessId) {
        if (businessRepo.findById(businessId).isEmpty()) {
            throw new NoSuchElementException("Business introuvable : " + businessId);
        }
    }

    private User requireUser(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable : " + userId));
    }

    private void requireAccount(UUID accountId, UUID businessId) {
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Compte introuvable : " + accountId));
        if (!account.getBusinessId().equals(businessId)) {
            throw new ForbiddenException("Ce compte n'appartient pas au business");
        }
    }
}
