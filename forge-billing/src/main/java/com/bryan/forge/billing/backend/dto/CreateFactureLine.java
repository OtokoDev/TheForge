package com.bryan.forge.billing.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.util.UUID;

/** Ligne à créer. {@code unitPrice} optionnel : surcharge le prix catalogue (négociation). */
@Serdeable
public record CreateFactureLine(UUID itemId, int quantity, @Nullable BigDecimal unitPrice) {}
