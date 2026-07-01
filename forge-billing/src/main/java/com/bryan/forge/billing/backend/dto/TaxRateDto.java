package com.bryan.forge.billing.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.time.Instant;

/** rate = part forgeron (sur le CA). cityFixed = forfait hebdo. cityRate = % du CA après paie forgerons. */
@Serdeable
public record TaxRateDto(BigDecimal rate, long cityFixed, BigDecimal cityRate, @Nullable Instant validFrom) {}
