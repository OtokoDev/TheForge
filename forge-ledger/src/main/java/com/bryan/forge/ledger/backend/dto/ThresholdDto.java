package com.bryan.forge.ledger.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

@Serdeable
public record ThresholdDto(UUID itemId, int minQty) {}
