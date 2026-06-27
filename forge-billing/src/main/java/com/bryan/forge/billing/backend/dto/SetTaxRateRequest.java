package com.bryan.forge.billing.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;

@Serdeable
public record SetTaxRateRequest(BigDecimal rate) {}
