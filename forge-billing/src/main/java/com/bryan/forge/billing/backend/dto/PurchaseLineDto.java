package com.bryan.forge.billing.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.util.UUID;

@Serdeable
public record PurchaseLineDto(UUID id, UUID itemId, String itemName, int quantity, BigDecimal unitCost, long lineTotal) {}
