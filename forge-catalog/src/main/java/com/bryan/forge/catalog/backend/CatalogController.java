package com.bryan.forge.catalog.backend;

import com.bryan.forge.catalog.backend.dto.CreateItemRequest;
import com.bryan.forge.catalog.backend.dto.ItemDto;
import com.bryan.forge.catalog.backend.dto.RecipeComponentDto;
import com.bryan.forge.catalog.backend.dto.RecipeEdgeDto;
import com.bryan.forge.catalog.backend.dto.SetRecipeRequest;
import com.bryan.forge.catalog.backend.dto.UpdateItemRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.Status;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.util.List;
import java.util.UUID;

/**
 * Catalogue global (items + recettes). Lecture pour tout utilisateur authentifié ;
 * écriture réservée au rôle global SYSTEM (cf. CDC §5.3).
 */
@Controller("/api/catalog")
@ExecuteOn(TaskExecutors.BLOCKING)
public class CatalogController {

    private final ItemService itemService;
    private final RecipeService recipeService;

    public CatalogController(ItemService itemService, RecipeService recipeService) {
        this.itemService = itemService;
        this.recipeService = recipeService;
    }

    // ── Items ─────────────────────────────────────────────────────────────────

    @Get("/items")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public List<ItemDto> listItems() {
        return itemService.listAll();
    }

    @Post("/items")
    @Status(HttpStatus.CREATED)
    @Secured("ROLE_SYSTEM")
    public ItemDto createItem(@Body CreateItemRequest req) {
        return itemService.create(req);
    }

    @Put("/items/{id}")
    @Secured("ROLE_SYSTEM")
    public ItemDto updateItem(UUID id, @Body UpdateItemRequest req) {
        return itemService.update(id, req);
    }

    @Delete("/items/{id}")
    @Status(HttpStatus.NO_CONTENT)
    @Secured("ROLE_SYSTEM")
    public void deleteItem(UUID id) {
        itemService.delete(id);
    }

    // ── Recettes ──────────────────────────────────────────────────────────────

    @Get("/recipes")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public List<RecipeEdgeDto> allRecipes() {
        return recipeService.allRecipes();
    }

    @Get("/items/{id}/recipe")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public List<RecipeComponentDto> getRecipe(UUID id) {
        return recipeService.getRecipe(id);
    }

    @Put("/items/{id}/recipe")
    @Secured("ROLE_SYSTEM")
    public List<RecipeComponentDto> setRecipe(UUID id, @Body SetRecipeRequest req) {
        return recipeService.setRecipe(id, req.components() == null ? List.of() : req.components());
    }
}
