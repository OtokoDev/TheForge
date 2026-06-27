package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.ItemCostDto;
import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datamodel.RecipeComponent;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.catalog.datarepository.RecipeComponentRepository;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.valuation.datamodel.Product;
import com.bryan.forge.valuation.datarepository.ProductRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Coût de revient récursif par business (CDC §6.4). Item composé → Σ coût(composant) ×
 * quantité, en descendant la recette ; item simple → sa valeur (valuation) locale, le
 * septime valant 1. Le graphe étant un DAG (garde-fou catalogue), la récursion termine.
 */
@Singleton
public class CostingService {

    private final RecipeComponentRepository recipeRepo;
    private final ItemRepository itemRepo;
    private final ProductRepository productRepo;
    private final BusinessRepository businessRepo;
    private final BusinessAccessService access;

    public CostingService(RecipeComponentRepository recipeRepo, ItemRepository itemRepo,
                          ProductRepository productRepo, BusinessRepository businessRepo,
                          BusinessAccessService access) {
        this.recipeRepo = recipeRepo;
        this.itemRepo = itemRepo;
        this.productRepo = productRepo;
        this.businessRepo = businessRepo;
        this.access = access;
    }

    @Transactional
    public ItemCostDto cost(User actor, UUID businessId, UUID itemId) {
        if (businessRepo.findById(businessId).isEmpty()) {
            throw new NoSuchElementException("Business introuvable : " + businessId);
        }
        access.requireView(actor, businessId);
        Item item = itemRepo.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item introuvable : " + itemId));
        return new ItemCostDto(itemId, item.getName(), compute(businessId, itemId, new HashMap<>()));
    }

    /** Coût de revient récursif, pour usage interne (factures). */
    @Transactional
    public BigDecimal costOf(UUID businessId, UUID itemId) {
        return compute(businessId, itemId, new HashMap<>());
    }

    /** Coût de revient de tous les items pour ce business (mémo partagé → valeur du stock). */
    @Transactional
    public List<ItemCostDto> costs(User actor, UUID businessId) {
        if (businessRepo.findById(businessId).isEmpty()) {
            throw new NoSuchElementException("Business introuvable : " + businessId);
        }
        access.requireView(actor, businessId);
        Map<UUID, BigDecimal> memo = new HashMap<>();
        return itemRepo.findAll().stream()
                .map(item -> new ItemCostDto(item.getId(), item.getName(), compute(businessId, item.getId(), memo)))
                .toList();
    }

    private BigDecimal compute(UUID businessId, UUID itemId, Map<UUID, BigDecimal> memo) {
        BigDecimal cached = memo.get(itemId);
        if (cached != null) return cached;

        List<RecipeComponent> components = recipeRepo.findByOutputItemId(itemId);
        BigDecimal result;
        if (components.isEmpty()) {
            // Item simple : sa valeur locale (le septime vaut 1, valeur non définie → 0).
            Item item = itemRepo.findById(itemId).orElse(null);
            if (item != null && item.isSystem()) {
                result = BigDecimal.ONE;
            } else {
                result = productRepo.findByBusinessIdAndItemIdAndValidToIsNull(businessId, itemId)
                        .map(Product::getValeur)
                        .filter(java.util.Objects::nonNull)
                        .orElse(BigDecimal.ZERO);
            }
        } else {
            BigDecimal sum = BigDecimal.ZERO;
            for (RecipeComponent rc : components) {
                BigDecimal componentCost = compute(businessId, rc.getComponentItem().getId(), memo);
                sum = sum.add(componentCost.multiply(BigDecimal.valueOf(rc.getQuantity())));
            }
            result = sum;
        }

        memo.put(itemId, result);
        return result;
    }
}
