package com.bryan.forge.valuation.backend.seed;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.util.List;

/** Schéma du fichier seed/skyrim-catalog.json (familles, matériaux, items + recettes + prix). */
@Serdeable
public record SeedData(List<SeedTaxon> families, List<SeedTaxon> materials, List<SeedItem> items) {

    @Serdeable
    public record SeedTaxon(String nom, @Nullable String couleur) {}

    @Serdeable
    public record SeedItem(
            String name,
            @Nullable String family,
            @Nullable String material,
            @Nullable String hand,
            @Nullable BigDecimal valeur,
            @Nullable BigDecimal prixRevente,
            @Nullable List<SeedComponent> recipe) {}

    @Serdeable
    public record SeedComponent(String component, int qty) {}
}
