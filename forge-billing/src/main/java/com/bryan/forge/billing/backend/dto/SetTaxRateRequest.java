package com.bryan.forge.billing.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;

/** rate = part forgeron (0..1). cityFixed = forfait hebdo taxe ville. cityRate = % taxe ville (0..1). */
@Serdeable
public record SetTaxRateRequest(BigDecimal rate, long cityFixed, BigDecimal cityRate) {}
