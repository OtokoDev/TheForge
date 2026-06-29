package com.bryan.forge.billing.backend.dto;

import com.bryan.forge.billing.datamodel.TaxBase;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.time.Instant;

/** Taux courant d'un business. validFrom null = aucun taux défini (défaut 0). */
@Serdeable
public record TaxRateDto(BigDecimal rate, TaxBase base, @Nullable Instant validFrom) {}
