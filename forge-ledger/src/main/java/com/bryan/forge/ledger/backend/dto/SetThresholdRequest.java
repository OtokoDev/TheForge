package com.bryan.forge.ledger.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record SetThresholdRequest(int minQty) {}
