package com.bryan.forge.ledger.backend;

import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.ledger.backend.dto.DefaultsDto;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.core.backend.ForbiddenException;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.core.realtime.RealtimeEvent;
import io.micronaut.context.event.ApplicationEventPublisher;
import com.bryan.forge.ledger.backend.dto.AccountDto;
import com.bryan.forge.ledger.backend.dto.InventoryCount;
import com.bryan.forge.ledger.backend.dto.InventoryResultDto;
import com.bryan.forge.ledger.backend.dto.ItemBalanceDto;
import com.bryan.forge.ledger.backend.dto.MovementDto;
import com.bryan.forge.ledger.backend.dto.RecordMovementRequest;
import com.bryan.forge.ledger.backend.dto.StockRowDto;
import com.bryan.forge.ledger.datamodel.Account;
import com.bryan.forge.ledger.datamodel.AccountKind;
import com.bryan.forge.ledger.datamodel.Movement;
import com.bryan.forge.ledger.datamodel.MovementType;
import com.bryan.forge.ledger.datarepository.AccountRepository;
import com.bryan.forge.ledger.datarepository.MovementRepository;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class LedgerService {

    private final AccountRepository accountRepo;
    private final MovementRepository movementRepo;
    private final ItemRepository itemRepo;
    private final BusinessRepository businessRepo;
    private final BusinessAccessService access;
    private final EntityManager em;
    private final ApplicationEventPublisher<Object> events;

    public LedgerService(AccountRepository accountRepo, MovementRepository movementRepo,
                         ItemRepository itemRepo, BusinessRepository businessRepo,
                         BusinessAccessService access, EntityManager em,
                         ApplicationEventPublisher<Object> events) {
        this.accountRepo = accountRepo;
        this.movementRepo = movementRepo;
        this.itemRepo = itemRepo;
        this.businessRepo = businessRepo;
        this.access = access;
        this.em = em;
        this.events = events;
    }

    /**
     * Verrou pessimiste (FOR UPDATE) sur les comptes touchés, avant lecture du solde :
     * sérialise les mouvements d'un même compte → la garde « stock négatif » est fiable
     * même en concurrence (cf. CDC §6.1, garantie du suivi de stock). Tri par id pour
     * éviter les interblocages.
     */
    private void lockAccounts(UUID... accountIds) {
        Arrays.stream(accountIds).filter(Objects::nonNull).distinct().sorted()
                .forEach(id -> em.find(Account.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    // ── Comptes ─────────────────────────────────────────────────────────────

    @Transactional
    public List<AccountDto> listAccounts(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        return accountRepo.findByBusinessId(businessId).stream().map(AccountDto::from).toList();
    }

    @Transactional
    public AccountDto createAccount(User actor, UUID businessId, String name, AccountKind kind) {
        requireBusiness(businessId);
        access.requireAdmin(actor, businessId);
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Le nom du compte est obligatoire");
        }
        if (kind == null) {
            throw new IllegalArgumentException("La nature du compte est obligatoire");
        }
        return AccountDto.from(accountRepo.save(new Account(businessId, name.trim(), kind)));
    }

    /** Supprime un coffre s'il est VIDE (aucun item, toutes quantités à 0) et n'est pas le principal. */
    @Transactional
    public void deleteAccount(User actor, UUID businessId, UUID accountId) {
        requireBusiness(businessId);
        access.requireAdmin(actor, businessId);
        requireAccount(accountId, businessId);
        Business b = businessRepo.findById(businessId)
                .orElseThrow(() -> new NoSuchElementException("Business introuvable : " + businessId));
        if (accountId.equals(b.getDefaultStockAccountId()) || accountId.equals(b.getDefaultCoffreAccountId())) {
            throw new IllegalStateException("Impossible de supprimer le coffre principal — désigne-en un autre d'abord.");
        }
        boolean nonEmpty = balanceMap(accountId).values().stream().anyMatch(v -> v != 0);
        if (nonEmpty) {
            throw new IllegalStateException("Le coffre doit être vide (toutes les quantités à 0) avant suppression.");
        }
        accountRepo.deleteById(accountId);
    }

    // ── Projections ─────────────────────────────────────────────────────────

    /** Soldes par item d'un compte (projection ; on n'expose que les soldes non nuls). */
    @Transactional
    public List<ItemBalanceDto> balances(User actor, UUID businessId, UUID accountId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        requireAccount(accountId, businessId);

        Map<UUID, String> itemNames = itemNames();
        return balanceMap(accountId).entrySet().stream()
                .filter(e -> e.getValue() != 0L)
                .map(e -> new ItemBalanceDto(e.getKey(), itemNames.getOrDefault(e.getKey(), "?"), e.getValue()))
                .sorted(Comparator.comparing(ItemBalanceDto::itemName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Stock agrégé : toutes les lignes (compte × item) à solde non nul du business. */
    @Transactional
    public List<StockRowDto> stock(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        Map<UUID, String> itemNames = itemNames();
        List<StockRowDto> rows = new ArrayList<>();
        for (Account account : accountRepo.findByBusinessId(businessId)) {
            for (Map.Entry<UUID, Long> e : balanceMap(account.getId()).entrySet()) {
                if (e.getValue() == 0L) continue;
                rows.add(new StockRowDto(account.getId(), account.getName(), e.getKey(),
                        itemNames.getOrDefault(e.getKey(), "?"), e.getValue()));
            }
        }
        return rows;
    }

    /**
     * Validation d'inventaire (ADMIN) : pour chaque compte×item compté, régularise
     * l'écart compté − système par un mouvement ADJUSTMENT. Renvoie le nb de lignes ajustées.
     */
    @Transactional
    public InventoryResultDto validateInventory(User actor, UUID businessId, List<InventoryCount> counts) {
        requireBusiness(businessId);
        access.requireAdmin(actor, businessId);
        if (counts == null || counts.isEmpty()) return new InventoryResultDto(0);

        int adjusted = 0;
        for (InventoryCount c : counts) {
            if (c.counted() < 0) {
                throw new IllegalArgumentException("Quantité comptée négative");
            }
            requireAccount(c.accountId(), businessId);
            long current = balanceMap(c.accountId()).getOrDefault(c.itemId(), 0L);
            long ecart = c.counted() - current;
            if (ecart == 0) continue;
            if (ecart > 0) {
                applyMovement(businessId, c.itemId(), (int) ecart, null, c.accountId(),
                        MovementType.ADJUSTMENT, "INVENTAIRE", null, "Inventaire", actor.getId());
            } else {
                applyMovement(businessId, c.itemId(), (int) -ecart, c.accountId(), null,
                        MovementType.ADJUSTMENT, "INVENTAIRE", null, "Inventaire", actor.getId());
            }
            adjusted++;
        }
        return new InventoryResultDto(adjusted);
    }

    /** Comptes par défaut du business (utilisés par le POS de facturation). */
    @Transactional
    public DefaultsDto getDefaults(User actor, UUID businessId) {
        access.requireView(actor, businessId);
        Business b = businessRepo.findById(businessId)
                .orElseThrow(() -> new NoSuchElementException("Business introuvable : " + businessId));
        return new DefaultsDto(b.getDefaultStockAccountId(), b.getDefaultCoffreAccountId());
    }

    @Transactional
    public DefaultsDto setDefaults(User actor, UUID businessId, UUID stockAccountId, UUID coffreAccountId) {
        access.requireAdmin(actor, businessId);
        Business b = businessRepo.findById(businessId)
                .orElseThrow(() -> new NoSuchElementException("Business introuvable : " + businessId));
        if (stockAccountId != null) requireAccount(stockAccountId, businessId);
        if (coffreAccountId != null) requireAccount(coffreAccountId, businessId);
        b.setDefaultStockAccountId(stockAccountId);
        b.setDefaultCoffreAccountId(coffreAccountId);
        b.setModifiedBy(actor.getId());
        businessRepo.update(b);
        return new DefaultsDto(stockAccountId, coffreAccountId);
    }

    // ── Journal ─────────────────────────────────────────────────────────────

    @Transactional
    public List<MovementDto> journal(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        Map<UUID, String> itemNames = itemNames();
        Map<UUID, String> accountNames = accountNames(businessId);
        return movementRepo.findByBusinessIdOrderByCreatedAtDesc(businessId).stream()
                .map(m -> toDto(m, itemNames, accountNames))
                .toList();
    }

    // ── Écriture ──────────────────────────────────────────────────────────────

    /**
     * Enregistre un mouvement. Valide les comptes (même business), refuse un stock
     * négatif sur le compte source. Append-only : aucune modification ultérieure.
     */
    @Transactional
    public MovementDto record(User actor, UUID businessId, RecordMovementRequest req) {
        requireBusiness(businessId);
        access.requireOperate(actor, businessId);

        if (req.quantity() <= 0) {
            throw new IllegalArgumentException("La quantité doit être positive");
        }
        if (req.type() == null) {
            throw new IllegalArgumentException("Le type de mouvement est obligatoire");
        }
        if (req.fromAccountId() == null && req.toAccountId() == null) {
            throw new IllegalArgumentException("Au moins un compte (source ou destination) est requis");
        }
        if (req.fromAccountId() != null && req.fromAccountId().equals(req.toAccountId())) {
            throw new IllegalArgumentException("Compte source et destination identiques");
        }

        Item item = itemRepo.findById(req.itemId())
                .orElseThrow(() -> new NoSuchElementException("Item introuvable : " + req.itemId()));
        if (req.fromAccountId() != null) requireAccount(req.fromAccountId(), businessId);
        if (req.toAccountId() != null) requireAccount(req.toAccountId(), businessId);

        lockAccounts(req.fromAccountId(), req.toAccountId());
        // Garde stock négatif : le compte source doit détenir assez de l'item.
        if (req.fromAccountId() != null) {
            long available = balanceMap(req.fromAccountId()).getOrDefault(req.itemId(), 0L);
            if (available < req.quantity()) {
                throw new IllegalStateException("Stock insuffisant pour " + item.getName()
                        + " : " + req.quantity() + " demandé(s), " + available + " disponible(s)");
            }
        }

        String note = req.note() == null || req.note().isBlank() ? null : req.note().trim();
        Movement saved = movementRepo.save(new Movement(
                businessId, req.itemId(), req.quantity(), req.fromAccountId(), req.toAccountId(),
                req.type(), "MANUAL", null, note, actor.getId()));
        events.publishEvent(new RealtimeEvent(businessId, "STOCK"));

        return toDto(saved, itemNames(), accountNames(businessId));
    }

    /**
     * Enregistre un mouvement SANS contrôle d'autorisation (l'appelant l'a déjà fait),
     * avec garde stock négatif sur le compte source. Pour usage interne (factures…).
     */
    @Transactional
    public Movement applyMovement(UUID businessId, UUID itemId, int quantity, UUID fromAccountId,
                                  UUID toAccountId, MovementType type, String referenceType,
                                  UUID referenceId, String note, UUID userId) {
        lockAccounts(fromAccountId, toAccountId);
        if (fromAccountId != null) {
            long available = balanceMap(fromAccountId).getOrDefault(itemId, 0L);
            if (available < quantity) {
                throw new IllegalStateException("Stock insuffisant : " + quantity
                        + " demandé(s), " + available + " disponible(s)");
            }
        }
        Movement saved = movementRepo.save(new Movement(businessId, itemId, quantity, fromAccountId, toAccountId,
                type, referenceType, referenceId, note, userId));
        events.publishEvent(new RealtimeEvent(businessId, "STOCK"));
        return saved;
    }

    /** Solde d'un item sur un compte (interne, sans contrôle d'accès — l'appelant l'a fait). */
    @Transactional
    public long balanceOf(UUID accountId, UUID itemId) {
        return balanceMap(accountId).getOrDefault(itemId, 0L);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /** Solde par item d'un compte = Σ(entrées vers le compte) − Σ(sorties du compte). */
    private Map<UUID, Long> balanceMap(UUID accountId) {
        Map<UUID, Long> balances = new HashMap<>();
        for (Movement m : movementRepo.findByFromAccountIdOrToAccountId(accountId, accountId)) {
            long sign = 0;
            if (accountId.equals(m.getToAccountId())) sign += 1;
            if (accountId.equals(m.getFromAccountId())) sign -= 1;
            balances.merge(m.getItemId(), sign * m.getQuantity(), Long::sum);
        }
        return balances;
    }

    private void requireBusiness(UUID businessId) {
        if (businessRepo.findById(businessId).isEmpty()) {
            throw new NoSuchElementException("Business introuvable : " + businessId);
        }
    }

    private Account requireAccount(UUID accountId, UUID businessId) {
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Compte introuvable : " + accountId));
        if (!account.getBusinessId().equals(businessId)) {
            throw new ForbiddenException("Ce compte n'appartient pas au business");
        }
        return account;
    }

    private Map<UUID, String> itemNames() {
        return itemRepo.findAll().stream().collect(Collectors.toMap(Item::getId, Item::getName));
    }

    private Map<UUID, String> accountNames(UUID businessId) {
        return accountRepo.findByBusinessId(businessId).stream()
                .collect(Collectors.toMap(Account::getId, Account::getName));
    }

    private MovementDto toDto(Movement m, Map<UUID, String> itemNames, Map<UUID, String> accountNames) {
        return new MovementDto(
                m.getId(),
                m.getItemId(),
                itemNames.getOrDefault(m.getItemId(), "?"),
                m.getQuantity(),
                m.getFromAccountId(),
                m.getFromAccountId() == null ? null : accountNames.get(m.getFromAccountId()),
                m.getToAccountId(),
                m.getToAccountId() == null ? null : accountNames.get(m.getToAccountId()),
                m.getType(),
                m.getNote(),
                m.getCreatedAt());
    }
}
