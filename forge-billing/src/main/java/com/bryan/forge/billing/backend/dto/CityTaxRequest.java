package com.bryan.forge.billing.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record CityTaxRequest(long amount) {}
