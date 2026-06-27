package com.bryan.forge.catalog.backend;

import com.bryan.forge.catalog.backend.dto.RecipeComponentDto;
import com.bryan.forge.catalog.backend.dto.RecipeLine;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datamodel.RecipeComponent;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.catalog.datarepository.RecipeComponentRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Singleton
public class RecipeService {

    private final ItemRepository itemRepo;
    private final RecipeComponentRepository recipeRepo;

    public RecipeService(ItemRepository itemRepo, RecipeComponentRepository recipeRepo) {
        this.itemRepo = itemRepo;
        this.recipeRepo = recipeRepo;
    }

    @Transactional
    public List<RecipeComponentDto> getRecipe(UUID outputItemId) {
        requireItem(outputItemId);
        return recipeRepo.findByOutputItemId(outputItemId).stream().map(RecipeComponentDto::from).toList();
    }

    /**
     * Remplace l'intégralité de la recette de {@code outputItemId}. Valide les composants
     * et refuse tout cycle (le graphe de recettes doit rester un DAG, cf. CDC §6.4).
     */
    @Transactional
    public List<RecipeComponentDto> setRecipe(UUID outputItemId, List<RecipeLine> lines) {
        Item output = requireItem(outputItemId);

        Set<UUID> seen = new HashSet<>();
        for (RecipeLine line : lines) {
            if (line.quantity() <= 0) {
                throw new IllegalArgumentException("La quantité d'un composant doit être positive");
            }
            if (line.componentItemId().equals(outputItemId)) {
                throw new IllegalArgumentException("Un item ne peut pas se composer de lui-même");
            }
            if (!seen.add(line.componentItemId())) {
                throw new IllegalArgumentException("Composant en double dans la recette");
            }
            requireItem(line.componentItemId());
        }

        // Anti-cycle : sur le graphe existant (privé des arêtes de output, qu'on remplace),
        // aucun composant proposé ne doit pouvoir atteindre output.
        Map<UUID, List<UUID>> graph = buildGraphExcluding(outputItemId);
        for (RecipeLine line : lines) {
            if (reaches(line.componentItemId(), outputItemId, graph)) {
                throw new IllegalArgumentException("Cycle de recette détecté : un composant dépend de l'item produit");
            }
        }

        recipeRepo.deleteByOutputItemId(outputItemId);
        for (RecipeLine line : lines) {
            Item component = itemRepo.findById(line.componentItemId()).orElseThrow();
            recipeRepo.save(new RecipeComponent(output, component, line.quantity()));
        }
        return getRecipe(outputItemId);
    }

    private Item requireItem(UUID id) {
        return itemRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Item introuvable : " + id));
    }

    /** Arêtes output→composant de toutes les recettes, sauf celles de {@code excludedOutput}. */
    private Map<UUID, List<UUID>> buildGraphExcluding(UUID excludedOutput) {
        Map<UUID, List<UUID>> graph = new HashMap<>();
        for (RecipeComponent rc : recipeRepo.findAll()) {
            UUID out = rc.getOutputItem().getId();
            if (out.equals(excludedOutput)) continue;
            graph.computeIfAbsent(out, k -> new ArrayList<>()).add(rc.getComponentItem().getId());
        }
        return graph;
    }

    /** Vrai si {@code target} est atteignable depuis {@code start} en suivant les arêtes. */
    private boolean reaches(UUID start, UUID target, Map<UUID, List<UUID>> graph) {
        Deque<UUID> stack = new ArrayDeque<>();
        Set<UUID> visited = new HashSet<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            UUID node = stack.pop();
            if (node.equals(target)) return true;
            if (!visited.add(node)) continue;
            for (UUID next : graph.getOrDefault(node, List.of())) {
                stack.push(next);
            }
        }
        return false;
    }
}
