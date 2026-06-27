package com.bryan.forge.catalog.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/** Remplace l'intégralité de la recette d'un item par cette liste de composants. */
@Serdeable
public record SetRecipeRequest(List<RecipeLine> components) {}
