package com.bryan.forge.catalog.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

/** Arête de recette à plat (output → composant) — pour le calcul « fabricable » côté POS. */
@Serdeable
public record RecipeEdgeDto(UUID outputItemId, UUID componentItemId, int quantity) {
}
