package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.CreateTradeRequest;
import com.bryan.forge.billing.backend.dto.TradeDto;
import com.bryan.forge.billing.datamodel.Trade;
import com.bryan.forge.billing.datamodel.TradeLine;
import com.bryan.forge.billing.datamodel.TradeStatus;
import com.bryan.forge.billing.datarepository.TradeLineRepository;
import com.bryan.forge.billing.datarepository.TradeRepository;
import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.core.backend.AuditService;
import com.bryan.forge.core.backend.ForbiddenException;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.core.realtime.RealtimeEvent;
import com.bryan.forge.ledger.backend.LedgerService;
import com.bryan.forge.ledger.datamodel.MovementType;
import io.micronaut.context.event.ApplicationEventPublisher;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Commerce inter-business (CDC §6.7). Le vendeur (from) propose des items contre des septims ;
 * un opérateur de l'acheteur (to) accepte → exécution atomique : marchandise stock→stock,
 * septims coffre→coffre (comptes par défaut). Chaque business voit son côté dans son journal.
 */
@Singleton
public class TradeService {

    private final TradeRepository tradeRepo;
    private final TradeLineRepository lineRepo;
    private final ItemRepository itemRepo;
    private final BusinessRepository businessRepo;
    private final BusinessAccessService access;
    private final LedgerService ledger;
    private final AuditService audit;
    private final ApplicationEventPublisher<Object> events;

    public TradeService(TradeRepository tradeRepo, TradeLineRepository lineRepo, ItemRepository itemRepo,
                        BusinessRepository businessRepo, BusinessAccessService access, LedgerService ledger,
                        AuditService audit, ApplicationEventPublisher<Object> events) {
        this.tradeRepo = tradeRepo;
        this.lineRepo = lineRepo;
        this.itemRepo = itemRepo;
        this.businessRepo = businessRepo;
        this.access = access;
        this.ledger = ledger;
        this.audit = audit;
        this.events = events;
    }

    @Transactional
    public List<TradeDto> list(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        Map<UUID, String> items = itemNames();
        return tradeRepo.findByFromBusinessIdOrToBusinessIdOrderByCreatedAtDesc(businessId, businessId).stream()
                .map(t -> toDto(t, items))
                .toList();
    }

    /** Proposition (vendeur). Aucun mouvement : le stock n'est vérifié qu'à l'acceptation. */
    @Transactional
    public TradeDto create(User actor, UUID businessId, CreateTradeRequest req) {
        requireBusiness(businessId);
        access.requireOperate(actor, businessId);
        if (req.toBusinessId() == null || req.toBusinessId().equals(businessId)) {
            throw new IllegalArgumentException("Business destinataire invalide");
        }
        requireBusiness(req.toBusinessId());
        if (req.lines() == null || req.lines().isEmpty()) {
            throw new IllegalArgumentException("Un échange doit avoir au moins une ligne");
        }
        if (req.septims() < 0) {
            throw new IllegalArgumentException("La contrepartie doit être positive ou nulle");
        }
        Trade trade = tradeRepo.save(new Trade(tradeRepo.nextNumero(), businessId, req.toBusinessId(),
                req.septims(), blankToNull(req.note()), actor.getId()));
        for (CreateTradeRequest.Line line : req.lines()) {
            if (line.quantity() <= 0) {
                throw new IllegalArgumentException("Quantité invalide");
            }
            itemRepo.findById(line.itemId())
                    .orElseThrow(() -> new NoSuchElementException("Item introuvable : " + line.itemId()));
            lineRepo.save(new TradeLine(trade.getId(), line.itemId(), line.quantity()));
        }
        audit.record(businessId, actor.getId(), "TRADE_PROPOSE",
                "Échange #" + trade.getNumero() + " proposé à " + businessName(req.toBusinessId()));
        notifyBoth(trade);
        return toDto(trade, itemNames());
    }

    /** Acceptation (acheteur) : exécute l'échange atomiquement. */
    @Transactional
    public TradeDto accept(User actor, UUID businessId, UUID tradeId) {
        Trade trade = require(tradeId);
        if (!trade.getToBusinessId().equals(businessId)) {
            throw new ForbiddenException("Seul le business destinataire peut accepter");
        }
        access.requireOperate(actor, businessId);
        requireProposed(trade);

        Business from = requireBusinessEntity(trade.getFromBusinessId());
        Business to = requireBusinessEntity(trade.getToBusinessId());
        UUID stockFrom = requireAccount(from.getDefaultStockAccountId(), "compte stock", from.getNom());
        UUID stockTo = requireAccount(to.getDefaultStockAccountId(), "compte stock", to.getNom());

        String ref = "Échange #" + trade.getNumero() + " (" + from.getNom() + " → " + to.getNom() + ")";
        // Marchandise : chaque business enregistre son côté dans son propre journal.
        for (TradeLine line : lineRepo.findByTradeId(tradeId)) {
            ledger.applyMovement(from.getId(), line.getItemId(), line.getQuantity(), stockFrom, null,
                    MovementType.TRANSFER, "TRADE", tradeId, ref, actor.getId());
            ledger.applyMovement(to.getId(), line.getItemId(), line.getQuantity(), null, stockTo,
                    MovementType.TRANSFER, "TRADE", tradeId, ref, actor.getId());
        }
        // Contrepartie : septims de l'acheteur vers le vendeur.
        if (trade.getSeptims() > 0) {
            UUID coffreFrom = requireAccount(from.getDefaultCoffreAccountId(), "coffre", from.getNom());
            UUID coffreTo = requireAccount(to.getDefaultCoffreAccountId(), "coffre", to.getNom());
            UUID septime = septimeId();
            ledger.applyMovement(to.getId(), septime, (int) trade.getSeptims(), coffreTo, null,
                    MovementType.TRANSFER, "TRADE", tradeId, ref, actor.getId());
            ledger.applyMovement(from.getId(), septime, (int) trade.getSeptims(), null, coffreFrom,
                    MovementType.TRANSFER, "TRADE", tradeId, ref, actor.getId());
        }

        trade.decide(TradeStatus.ACCEPTEE, actor.getId());
        tradeRepo.update(trade);
        audit.record(businessId, actor.getId(), "TRADE_ACCEPTE", ref + " accepté");
        notifyBoth(trade);
        return toDto(trade, itemNames());
    }

    /** Refus (acheteur). */
    @Transactional
    public TradeDto refuse(User actor, UUID businessId, UUID tradeId) {
        Trade trade = require(tradeId);
        if (!trade.getToBusinessId().equals(businessId)) {
            throw new ForbiddenException("Seul le business destinataire peut refuser");
        }
        access.requireOperate(actor, businessId);
        requireProposed(trade);
        trade.decide(TradeStatus.REFUSEE, actor.getId());
        tradeRepo.update(trade);
        notifyBoth(trade);
        return toDto(trade, itemNames());
    }

    /** Annulation (vendeur, tant que non décidé). */
    @Transactional
    public TradeDto cancel(User actor, UUID businessId, UUID tradeId) {
        Trade trade = require(tradeId);
        if (!trade.getFromBusinessId().equals(businessId)) {
            throw new ForbiddenException("Seul le business émetteur peut annuler");
        }
        access.requireOperate(actor, businessId);
        requireProposed(trade);
        trade.decide(TradeStatus.ANNULEE, actor.getId());
        tradeRepo.update(trade);
        notifyBoth(trade);
        return toDto(trade, itemNames());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void notifyBoth(Trade trade) {
        events.publishEvent(new RealtimeEvent(trade.getFromBusinessId(), "TRADE"));
        events.publishEvent(new RealtimeEvent(trade.getToBusinessId(), "TRADE"));
    }

    private static void requireProposed(Trade trade) {
        if (trade.getStatus() != TradeStatus.PROPOSEE) {
            throw new IllegalStateException("Échange déjà décidé");
        }
    }

    private Trade require(UUID tradeId) {
        return tradeRepo.findById(tradeId)
                .orElseThrow(() -> new NoSuchElementException("Échange introuvable : " + tradeId));
    }

    private void requireBusiness(UUID businessId) {
        if (businessRepo.findById(businessId).isEmpty()) {
            throw new NoSuchElementException("Business introuvable : " + businessId);
        }
    }

    private Business requireBusinessEntity(UUID businessId) {
        return businessRepo.findById(businessId)
                .orElseThrow(() -> new NoSuchElementException("Business introuvable : " + businessId));
    }

    private static UUID requireAccount(UUID accountId, String kind, String businessName) {
        if (accountId == null) {
            throw new IllegalStateException("Aucun " + kind + " par défaut chez " + businessName);
        }
        return accountId;
    }

    private UUID septimeId() {
        return itemRepo.findFirstBySystemTrue()
                .orElseThrow(() -> new IllegalStateException("Item septime introuvable")).getId();
    }

    private String businessName(UUID id) {
        return businessRepo.findById(id).map(Business::getNom).orElse("?");
    }

    private Map<UUID, String> itemNames() {
        return itemRepo.findAll().stream().collect(Collectors.toMap(Item::getId, Item::getName));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private TradeDto toDto(Trade t, Map<UUID, String> items) {
        List<TradeDto.TradeLineDto> lines = lineRepo.findByTradeId(t.getId()).stream()
                .map(l -> new TradeDto.TradeLineDto(l.getItemId(), items.getOrDefault(l.getItemId(), "?"), l.getQuantity()))
                .toList();
        return new TradeDto(t.getId(), t.getNumero(), t.getFromBusinessId(), businessName(t.getFromBusinessId()),
                t.getToBusinessId(), businessName(t.getToBusinessId()), t.getStatus(), t.getSeptims(),
                t.getNote(), t.getCreatedAt(), lines);
    }
}
