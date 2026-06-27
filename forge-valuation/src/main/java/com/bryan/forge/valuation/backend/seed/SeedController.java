package com.bryan.forge.valuation.backend.seed;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;

import java.util.UUID;

/**
 * Import du catalogue Skyrim (familles, matériaux, items, recettes). Réservé à SYSTEM,
 * idempotent. {@code businessId} optionnel : seed aussi les prix (produits) pour ce business.
 */
@Controller("/api/catalog/seed")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured("ROLE_SYSTEM")
public class SeedController {

    private final CatalogSeedService seedService;

    public SeedController(CatalogSeedService seedService) {
        this.seedService = seedService;
    }

    @Post
    public SeedResult seed(@Nullable @QueryValue UUID businessId) {
        return seedService.run(businessId);
    }
}
