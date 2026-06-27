package com.bryan.forge.valuation.backend;

import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.catalog.datarepository.RecipeComponentRepository;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.valuation.backend.dto.ProductDto;
import com.bryan.forge.valuation.backend.dto.ProductHistoryDto;
import com.bryan.forge.valuation.datamodel.Product;
import com.bryan.forge.valuation.datarepository.ProductRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Produits PAR BUSINESS (historisés) : valeur (coût, matières) + prix de revente.
 * Lecture pour les membres ; écriture ADMIN du business. La valeur est dérivée (non
 * stockée) pour un craftable ; on ne la saisit que sur les matières (feuilles).
 */
@Singleton
public class ProductService {

    private final ProductRepository repo;
    private final ItemRepository itemRepo;
    private final RecipeComponentRepository recipeRepo;
    private final BusinessRepository businessRepo;
    private final BusinessAccessService access;

    public ProductService(ProductRepository repo, ItemRepository itemRepo, RecipeComponentRepository recipeRepo,
                          BusinessRepository businessRepo, BusinessAccessService access) {
        this.repo = repo;
        this.itemRepo = itemRepo;
        this.recipeRepo = recipeRepo;
        this.businessRepo = businessRepo;
        this.access = access;
    }

    /** Produits courants de tous les items pour ce business (septime : valeur = 1). */
    @Transactional
    public List<ProductDto> listCurrent(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);

        Map<UUID, Product> current = repo.findByBusinessIdAndValidToIsNull(businessId).stream()
                .collect(Collectors.toMap(Product::getItemId, p -> p));
        Set<UUID> withRecipe = recipeRepo.findAll().stream()
                .map(rc -> rc.getOutputItem().getId())
                .collect(Collectors.toSet());

        return itemRepo.findAll().stream()
                .map(item -> {
                    boolean hasRecipe = withRecipe.contains(item.getId());
                    if (item.isSystem()) {
                        return new ProductDto(item.getId(), item.getName(), false, BigDecimal.ONE, null, null, 0);
                    }
                    Product p = current.get(item.getId());
                    return p == null ? null
                            : new ProductDto(item.getId(), item.getName(), hasRecipe, p.getValeur(), p.getPrixRevente(), p.getValidFrom(), p.getVersion());
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(ProductDto::itemName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Définit le produit courant : clôture la version précédente, en crée une nouvelle.
     * Valeur saisie uniquement pour les matières (craftable → forcée null) ; prix de revente
     * indépendant (null → proposé = valeur).
     */
    @Transactional
    public ProductDto setProduct(User actor, UUID businessId, UUID itemId, BigDecimal valeur, BigDecimal prixRevente, int version) {
        requireBusiness(businessId);
        access.requireAdmin(actor, businessId);

        Item item = itemRepo.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item introuvable : " + itemId));
        if (item.isSystem()) {
            throw new IllegalStateException("La valeur du septime est fixée à 1");
        }
        boolean hasRecipe = recipeRepo.existsByOutputItemId(itemId);
        // Valeur saisie seulement pour les matières ; craftable → dérivée (non stockée).
        BigDecimal valeurToStore = hasRecipe ? null : valeur;
        if (valeurToStore != null && valeurToStore.signum() < 0) {
            throw new IllegalArgumentException("La valeur doit être positive ou nulle");
        }
        // Première complétion : prix de revente nul → proposé = valeur.
        BigDecimal prixToStore = prixRevente != null ? prixRevente : valeurToStore;
        if (prixToStore != null && prixToStore.signum() < 0) {
            throw new IllegalArgumentException("Le prix de revente doit être positif ou nul");
        }

        repo.findByBusinessIdAndItemIdAndValidToIsNull(businessId, itemId).ifPresent(currentVal -> {
            com.bryan.forge.core.backend.StaleDataException.check(currentVal.getVersion(), version);
            currentVal.setValidTo(Instant.now());
            repo.update(currentVal);
        });

        Product product = new Product(businessId, itemId, valeurToStore, prixToStore);
        product.setCreatedBy(actor.getId());
        product.setModifiedBy(actor.getId());
        Product saved = repo.save(product);
        return new ProductDto(item.getId(), item.getName(), hasRecipe, saved.getValeur(), saved.getPrixRevente(), saved.getValidFrom(), saved.getVersion());
    }

    /** Historique d'un item dans ce business (plus récent en premier). */
    @Transactional
    public List<ProductHistoryDto> history(User actor, UUID businessId, UUID itemId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        return repo.findByBusinessIdAndItemIdOrderByValidFromDesc(businessId, itemId).stream()
                .map(ProductHistoryDto::from)
                .toList();
    }

    private void requireBusiness(UUID businessId) {
        if (!businessRepo.findById(businessId).isPresent()) {
            throw new NoSuchElementException("Business introuvable : " + businessId);
        }
    }
}
