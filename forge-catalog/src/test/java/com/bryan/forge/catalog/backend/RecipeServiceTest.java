package com.bryan.forge.catalog.backend;

import com.bryan.forge.catalog.backend.dto.RecipeLine;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datamodel.RecipeComponent;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.catalog.datarepository.RecipeComponentRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecipeServiceTest {

    private final ItemRepository itemRepo = mock(ItemRepository.class);
    private final RecipeComponentRepository recipeRepo = mock(RecipeComponentRepository.class);
    private final RecipeService service = new RecipeService(itemRepo, recipeRepo);

    @Test
    void refuseUnComposantEgalAuProduit() {
        UUID a = UUID.randomUUID();
        Item ia = item(a);
        when(itemRepo.findById(a)).thenReturn(Optional.of(ia));
        when(recipeRepo.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.setRecipe(a, List.of(new RecipeLine(a, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lui-même");
        verify(recipeRepo, never()).save(any());
    }

    @Test
    void refuseUnCycle() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Item ia = item(a);
        Item ib = item(b);
        RecipeComponent existing = edge(ib, ia); // B requiert A
        when(itemRepo.findById(a)).thenReturn(Optional.of(ia));
        when(itemRepo.findById(b)).thenReturn(Optional.of(ib));
        when(recipeRepo.findAll()).thenReturn(List.of(existing));

        // Tenter A requiert B → cycle A -> B -> A
        assertThatThrownBy(() -> service.setRecipe(a, List.of(new RecipeLine(b, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cycle");
        verify(recipeRepo, never()).save(any());
    }

    @Test
    void enregistreUneRecetteValide() {
        UUID sword = UUID.randomUUID();
        UUID ingot = UUID.randomUUID();
        Item iSword = item(sword);
        Item iIngot = item(ingot);
        when(itemRepo.findById(sword)).thenReturn(Optional.of(iSword));
        when(itemRepo.findById(ingot)).thenReturn(Optional.of(iIngot));
        when(recipeRepo.findAll()).thenReturn(List.of());
        when(recipeRepo.findByOutputItemId(sword)).thenReturn(List.of());

        service.setRecipe(sword, List.of(new RecipeLine(ingot, 3)));

        verify(recipeRepo).deleteByOutputItemId(sword);
        verify(recipeRepo).save(any(RecipeComponent.class));
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private Item item(UUID id) {
        Item i = mock(Item.class);
        when(i.getId()).thenReturn(id);
        return i;
    }

    private RecipeComponent edge(Item output, Item component) {
        RecipeComponent rc = mock(RecipeComponent.class);
        when(rc.getOutputItem()).thenReturn(output);
        when(rc.getComponentItem()).thenReturn(component);
        return rc;
    }
}
