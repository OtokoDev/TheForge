package com.bryan.forge.valuation.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;

/** Valeur (matières) + prix de revente, tous deux optionnels. */
@Serdeable
public record SetProductRequest(@Nullable BigDecimal valeur, @Nullable BigDecimal prixRevente, int version) {}
