package com.bryan.forge.valuation.backend.seed;

import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.backend.RecipeService;
import com.bryan.forge.catalog.backend.dto.RecipeLine;
import com.bryan.forge.catalog.datamodel.HandRequired;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datamodel.Taxon;
import com.bryan.forge.catalog.datamodel.TaxonKind;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.catalog.datarepository.TaxonRepository;
import com.bryan.forge.valuation.datamodel.Product;
import com.bryan.forge.valuation.datarepository.ProductRepository;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Importe le catalogue Skyrim (seed/skyrim-catalog.json) : familles, matériaux, items,
 * recettes, et — si un business est fourni — les prix (produits). Idempotent : crée par
 * nom ce qui manque, ne touche pas l'existant. Réservé à SYSTEM (porté par le contrôleur).
 */
@Singleton
public class CatalogSeedService {

    private static final String RESOURCE = "/seed/skyrim-catalog.json";

    private final TaxonRepository taxonRepo;
    private final ItemRepository itemRepo;
    private final RecipeService recipeService;
    private final ProductRepository productRepo;
    private final BusinessRepository businessRepo;
    private final ObjectMapper objectMapper;

    public CatalogSeedService(TaxonRepository taxonRepo, ItemRepository itemRepo, RecipeService recipeService,
                              ProductRepository productRepo, BusinessRepository businessRepo, ObjectMapper objectMapper) {
        this.taxonRepo = taxonRepo;
        this.itemRepo = itemRepo;
        this.recipeService = recipeService;
        this.productRepo = productRepo;
        this.businessRepo = businessRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SeedResult run(UUID businessId) {
        if (businessId != null && businessRepo.findById(businessId).isEmpty()) {
            throw new NoSuchElementException("Business introuvable : " + businessId);
        }
        SeedData data = load();
        List<String> warnings = new ArrayList<>();

        UpsertResult fam = upsertTaxa(TaxonKind.FAMILY, data.families());
        UpsertResult mat = upsertTaxa(TaxonKind.MATERIAL, data.materials());

        // Items (par nom).
        Map<String, UUID> itemIds = new HashMap<>();
        for (Item i : itemRepo.findAll()) itemIds.put(i.getName(), i.getId());
        int itemsCreated = 0;
        for (SeedData.SeedItem si : data.items()) {
            if (itemIds.containsKey(si.name())) continue;
            UUID familyId = resolve(fam.ids, si.family(), "famille", si.name(), warnings);
            UUID materialId = resolve(mat.ids, si.material(), "matériau", si.name(), warnings);
            HandRequired hand = si.hand() == null ? null : HandRequired.valueOf(si.hand());
            Item saved = itemRepo.save(new Item(si.name(), familyId, materialId, hand));
            itemIds.put(si.name(), saved.getId());
            itemsCreated++;
        }

        // Recettes (tous les items existent désormais).
        int recipesSet = 0;
        for (SeedData.SeedItem si : data.items()) {
            if (si.recipe() == null || si.recipe().isEmpty()) continue;
            List<RecipeLine> lines = new ArrayList<>();
            boolean ok = true;
            for (SeedData.SeedComponent c : si.recipe()) {
                UUID cid = itemIds.get(c.component());
                if (cid == null) {
                    warnings.add("Recette « " + si.name() + " » : composant inconnu « " + c.component() + " »");
                    ok = false;
                    break;
                }
                lines.add(new RecipeLine(cid, c.qty()));
            }
            if (ok) {
                recipeService.setRecipe(itemIds.get(si.name()), lines);
                recipesSet++;
            }
        }

        // Prix (produits) pour le business cible, si fourni.
        int productsCreated = 0;
        if (businessId != null) {
            for (SeedData.SeedItem si : data.items()) {
                boolean hasRecipe = si.recipe() != null && !si.recipe().isEmpty();
                // Valeur (coût) uniquement sur les feuilles ; craftable → dérivée.
                var valeur = hasRecipe ? null : si.valeur();
                if (valeur == null && si.prixRevente() == null) continue;
                UUID itemId = itemIds.get(si.name());
                if (productRepo.findByBusinessIdAndItemIdAndValidToIsNull(businessId, itemId).isPresent()) continue;
                productRepo.save(new Product(businessId, itemId, valeur, si.prixRevente()));
                productsCreated++;
            }
        }

        return new SeedResult(fam.created, mat.created, itemsCreated, recipesSet, productsCreated, warnings);
    }

    private UUID resolve(Map<String, UUID> ids, String name, String kind, String item, List<String> warnings) {
        if (name == null) return null;
        UUID id = ids.get(name);
        if (id == null) warnings.add("Item « " + item + " » : " + kind + " inconnu « " + name + " »");
        return id;
    }

    private UpsertResult upsertTaxa(TaxonKind kind, List<SeedData.SeedTaxon> list) {
        Map<String, UUID> ids = new HashMap<>();
        for (Taxon t : taxonRepo.findByKind(kind)) ids.put(t.getNom(), t.getId());
        int created = 0;
        for (int i = 0; i < list.size(); i++) {
            SeedData.SeedTaxon t = list.get(i);
            if (ids.containsKey(t.nom())) continue;
            Taxon saved = taxonRepo.save(new Taxon(kind, t.nom(), i, t.couleur()));
            ids.put(t.nom(), saved.getId());
            created++;
        }
        return new UpsertResult(ids, created);
    }

    private SeedData load() {
        try (InputStream in = getClass().getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException("Ressource introuvable : " + RESOURCE);
            return objectMapper.readValue(in.readAllBytes(), SeedData.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du seed impossible", e);
        }
    }

    private record UpsertResult(Map<String, UUID> ids, int created) {}
}
