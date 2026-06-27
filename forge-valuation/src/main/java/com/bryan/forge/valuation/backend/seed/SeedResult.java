package com.bryan.forge.valuation.backend.seed;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/** Compte-rendu d'un import de seed (idempotent). */
@Serdeable
public record SeedResult(int familiesCreated, int materialsCreated, int itemsCreated,
                         int recipesSet, int productsCreated, List<String> warnings) {}
