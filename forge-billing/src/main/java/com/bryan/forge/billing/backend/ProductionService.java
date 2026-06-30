package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.CreateProductionRequest;
import com.bryan.forge.billing.backend.dto.ProductionOrderDto;
import com.bryan.forge.billing.datamodel.ProductionOrder;
import com.bryan.forge.billing.datamodel.ProductionStatus;
import com.bryan.forge.billing.datarepository.ProductionOrderRepository;
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
import com.bryan.forge.ledger.datamodel.MovementType;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Atelier : ordres de fabrication. À la clôture, consomme les ingrédients de la recette
 * (1 niveau) depuis le stock par défaut et y fait entrer l'objet produit (atomique).
 */
@Singleton
public class ProductionService {

    private final ProductionOrderRepository repo;
    private final RecipeComponentRepository recipeRepo;
    private final ItemRepository itemRepo;
    private final BusinessRepository businessRepo;
    private final BusinessAccessService access;
    private final LedgerService ledgerService;

    public ProductionService(ProductionOrderRepository repo, RecipeComponentRepository recipeRepo,
                             ItemRepository itemRepo, BusinessRepository businessRepo,
                             BusinessAccessService access, LedgerService ledgerService) {
        this.repo = repo;
        this.recipeRepo = recipeRepo;
        this.itemRepo = itemRepo;
        this.businessRepo = businessRepo;
        this.access = access;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public List<ProductionOrderDto> list(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        Map<UUID, String> names = itemNames();
        return repo.findByBusinessIdOrderByNumeroDesc(businessId).stream().map(o -> toDto(o, names)).toList();
    }

    @Transactional
    public ProductionOrderDto create(User actor, UUID businessId, CreateProductionRequest req) {
        requireBusiness(businessId);
        access.requireOperate(actor, businessId);
        if (req.quantity() <= 0) {
            throw new IllegalArgumentException("Quantité invalide");
        }
        itemRepo.findById(req.outputItemId())
                .orElseThrow(() -> new NoSuchElementException("Objet introuvable : " + req.outputItemId()));
        if (recipeRepo.findByOutputItemId(req.outputItemId()).isEmpty()) {
            throw new IllegalArgumentException("Cet objet n'a pas de recette — fabrication impossible");
        }
        ProductionOrder o = repo.save(new ProductionOrder(businessId, repo.nextNumero(),
                req.outputItemId(), req.quantity(), actor.getId(), req.assignedTo()));
        return toDto(o, itemNames());
    }

    @Transactional
    public ProductionOrderDto start(User actor, UUID businessId, UUID id) {
        ProductionOrder o = forUpdate(actor, businessId, id);
        if (o.getStatus() != ProductionStatus.EN_ATTENTE) {
            throw new IllegalStateException("Seul un ordre en attente peut démarrer");
        }
        o.setStatus(ProductionStatus.EN_COURS);
        repo.update(o);
        return toDto(o, itemNames());
    }

    /** Clôture : consomme les ingrédients, fait entrer l'objet produit dans le stock. */
    @Transactional
    public ProductionOrderDto complete(User actor, UUID businessId, UUID id) {
        ProductionOrder o = forUpdate(actor, businessId, id);
        if (o.getStatus() == ProductionStatus.TERMINEE || o.getStatus() == ProductionStatus.ANNULEE) {
            throw new IllegalStateException("Ordre déjà terminé");
        }
        Business b = businessRepo.findById(businessId)
                .orElseThrow(() -> new NoSuchElementException("Business introuvable : " + businessId));
        UUID stock = b.getDefaultStockAccountId();
        if (stock == null) {
            throw new IllegalStateException("Aucun compte stock par défaut — configure-le d'abord.");
        }
        List<RecipeComponent> recipe = recipeRepo.findByOutputItemId(o.getOutputItemId());
        if (recipe.isEmpty()) {
            throw new IllegalStateException("Pas de recette — fabrication impossible");
        }
        String ref = "Fabrication #" + o.getNumero();
        // Consomme les ingrédients (garde stock négatif → refuse si une matière manque).
        for (RecipeComponent rc : recipe) {
            ledgerService.applyMovement(businessId, rc.getComponentItem().getId(),
                    rc.getQuantity() * o.getQuantity(), stock, null,
                    MovementType.CONSUMPTION, "PRODUCTION", o.getId(), ref, actor.getId());
        }
        // Fait entrer l'objet produit.
        ledgerService.applyMovement(businessId, o.getOutputItemId(), o.getQuantity(), null, stock,
                MovementType.PRODUCTION, "PRODUCTION", o.getId(), ref, actor.getId());

        o.setStatus(ProductionStatus.TERMINEE);
        o.setCompletedAt(Instant.now());
        repo.update(o);
        return toDto(o, itemNames());
    }

    @Transactional
    public ProductionOrderDto cancel(User actor, UUID businessId, UUID id) {
        ProductionOrder o = forUpdate(actor, businessId, id);
        if (o.getStatus() == ProductionStatus.TERMINEE) {
            throw new IllegalStateException("Ordre terminé — non annulable");
        }
        o.setStatus(ProductionStatus.ANNULEE);
        repo.update(o);
        return toDto(o, itemNames());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private ProductionOrder forUpdate(User actor, UUID businessId, UUID id) {
        requireBusiness(businessId);
        access.requireOperate(actor, businessId);
        return require(id, businessId);
    }

    private void requireBusiness(UUID businessId) {
        if (businessRepo.findById(businessId).isEmpty()) {
            throw new NoSuchElementException("Business introuvable : " + businessId);
        }
    }

    private ProductionOrder require(UUID id, UUID businessId) {
        ProductionOrder o = repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Ordre introuvable : " + id));
        if (!o.getBusinessId().equals(businessId)) {
            throw new ForbiddenException("Cet ordre n'appartient pas au business");
        }
        return o;
    }

    private Map<UUID, String> itemNames() {
        return itemRepo.findAll().stream().collect(Collectors.toMap(Item::getId, Item::getName));
    }

    private ProductionOrderDto toDto(ProductionOrder o, Map<UUID, String> names) {
        return new ProductionOrderDto(o.getId(), o.getNumero(), o.getOutputItemId(),
                names.getOrDefault(o.getOutputItemId(), "?"), o.getQuantity(), o.getStatus(),
                o.getAssignedTo(), o.getCreatedAt(), o.getCompletedAt());
    }
}
