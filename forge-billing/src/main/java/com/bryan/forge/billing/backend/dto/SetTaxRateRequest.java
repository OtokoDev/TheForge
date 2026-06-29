package com.bryan.forge.billing.backend.dto;

import com.bryan.forge.billing.datamodel.TaxBase;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;

/** {@code base} optionnel : PROFIT par défaut côté service. */
@Serdeable
public record SetTaxRateRequest(BigDecimal rate, @Nullable TaxBase base) {}
