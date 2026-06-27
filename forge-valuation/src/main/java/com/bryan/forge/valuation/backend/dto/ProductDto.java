package com.bryan.forge.valuation.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Produit courant d'un business. {@code valeur} = base de coût (matières) ;
 * {@code prixRevente} = prix de vente (null = non vendable). {@code hasRecipe} indique
 * un craftable (valeur dérivée, non saisissable). {@code validFrom} null = règle (septime).
 */
@Serdeable
public record ProductDto(
        UUID itemId,
        String itemName,
        boolean hasRecipe,
        @Nullable BigDecimal valeur,
        @Nullable BigDecimal prixRevente,
        @Nullable Instant validFrom,
        int version) {}
