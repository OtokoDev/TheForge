package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.CreateFactureLine;
import com.bryan.forge.billing.backend.dto.CreatePurchaseRequest;
import com.bryan.forge.billing.backend.dto.PurchaseDto;
import com.bryan.forge.billing.backend.dto.PurchaseLineDto;
import com.bryan.forge.billing.datamodel.Purchase;
import com.bryan.forge.billing.datamodel.PurchaseLine;
import com.bryan.forge.billing.datarepository.PurchaseLineRepository;
import com.bryan.forge.billing.datarepository.PurchaseRepository;
import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.core.backend.ForbiddenException;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.ledger.backend.LedgerService;
import com.bryan.forge.ledger.datamodel.MovementType;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Achats fournisseurs : paie les septimes depuis le coffre par défaut et fait entrer les
 * matières dans le stock par défaut (atomique ; garde solde négatif sur le coffre).
 */
@Singleton
public class PurchaseService {

    private final PurchaseRepository purchaseRepo;
    private final PurchaseLineRepository lineRepo;
    private final ItemRepository itemRepo;
    private final BusinessRepository businessRepo;
    private final BusinessAccessService access;
    private final LedgerService ledgerService;

    public PurchaseService(PurchaseRepository purchaseRepo, PurchaseLineRepository lineRepo,
                           ItemRepository itemRepo, BusinessRepository businessRepo,
                           BusinessAccessService access, LedgerService ledgerService) {
        this.purchaseRepo = purchaseRepo;
        this.lineRepo = lineRepo;
        this.itemRepo = itemRepo;
        this.businessRepo = businessRepo;
        this.access = access;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public List<PurchaseDto> list(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        Map<UUID, String> names = itemNames();
        return purchaseRepo.findByBusinessIdOrderByNumeroDesc(businessId).stream()
                .map(p -> toDto(p, lineRepo.findByPurchaseId(p.getId()), names))
                .toList();
    }

    @Transactional
    public PurchaseDto create(User actor, UUID businessId, CreatePurchaseRequest req) {
        requireBusiness(businessId);
        access.requireOperate(actor, businessId);
        if (req.lines() == null || req.lines().isEmpty()) {
            throw new IllegalArgumentException("Un achat doit avoir au moins une ligne");
        }
        Business b = businessRepo.findById(businessId)
                .orElseThrow(() -> new NoSuchElementException("Business introuvable : " + businessId));
        UUID coffre = b.getDefaultCoffreAccountId();
        UUID stock = b.getDefaultStockAccountId();
        if (coffre == null || stock == null) {
            throw new IllegalStateException("Coffre et/ou stock par défaut non configurés.");
        }

        long total = 0;
        for (CreateFactureLine line : req.lines()) {
            if (line.quantity() <= 0) throw new IllegalArgumentException("Quantité invalide");
            itemRepo.findById(line.itemId())
                    .orElseThrow(() -> new NoSuchElementException("Item introuvable : " + line.itemId()));
            total += lineTotal(line);
        }

        String supplier = req.supplierName() == null || req.supplierName().isBlank() ? null : req.supplierName().trim();
        Purchase p = purchaseRepo.save(new Purchase(businessId, purchaseRepo.nextNumero(), supplier, total, actor.getId()));
        String ref = "Achat #" + p.getNumero() + (supplier == null ? "" : " — " + supplier);

        // Paie les septimes (garde solde négatif sur le coffre → refuse si fonds insuffisants).
        if (total > 0) {
            ledgerService.applyMovement(businessId, septimeId(), (int) total, coffre, null,
                    MovementType.PURCHASE, "ACHAT", p.getId(), ref, actor.getId());
        }
        // Fait entrer les matières au stock.
        for (CreateFactureLine line : req.lines()) {
            BigDecimal unitCost = unitCost(line);
            lineRepo.save(new PurchaseLine(p.getId(), line.itemId(), line.quantity(), unitCost));
            ledgerService.applyMovement(businessId, line.itemId(), line.quantity(), null, stock,
                    MovementType.PURCHASE, "ACHAT", p.getId(), ref, actor.getId());
        }
        return toDto(p, lineRepo.findByPurchaseId(p.getId()), itemNames());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static BigDecimal unitCost(CreateFactureLine line) {
        return line.unitPrice() != null && line.unitPrice().signum() >= 0 ? line.unitPrice() : BigDecimal.ZERO;
    }

    private static long lineTotal(CreateFactureLine line) {
        return unitCost(line).multiply(BigDecimal.valueOf(line.quantity())).setScale(0, RoundingMode.CEILING).longValueExact();
    }

    private UUID septimeId() {
        return itemRepo.findFirstBySystemTrue()
                .orElseThrow(() -> new IllegalStateException("Item septime introuvable")).getId();
    }

    private void requireBusiness(UUID businessId) {
        if (businessRepo.findById(businessId).isEmpty()) {
            throw new NoSuchElementException("Business introuvable : " + businessId);
        }
    }

    private Map<UUID, String> itemNames() {
        return itemRepo.findAll().stream().collect(Collectors.toMap(Item::getId, Item::getName));
    }

    private PurchaseDto toDto(Purchase p, List<PurchaseLine> lines, Map<UUID, String> names) {
        List<PurchaseLineDto> lineDtos = lines.stream().map(l -> {
            long lt = l.getUnitCost().multiply(BigDecimal.valueOf(l.getQuantity()))
                    .setScale(0, RoundingMode.CEILING).longValueExact();
            return new PurchaseLineDto(l.getId(), l.getItemId(), names.getOrDefault(l.getItemId(), "?"),
                    l.getQuantity(), l.getUnitCost(), lt);
        }).toList();
        return new PurchaseDto(p.getId(), p.getNumero(), p.getSupplierName(), p.getTotal(),
                p.getCreatedAt(), lineDtos);
    }
}
