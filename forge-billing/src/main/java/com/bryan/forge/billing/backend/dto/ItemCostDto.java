package com.bryan.forge.billing.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.util.UUID;

/** Coût de revient récursif d'un item dans un business (Σ valeurs des composants). */
@Serdeable
public record ItemCostDto(UUID itemId, String itemName, BigDecimal cost) {}
