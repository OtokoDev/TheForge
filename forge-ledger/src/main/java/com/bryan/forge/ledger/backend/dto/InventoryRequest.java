package com.bryan.forge.ledger.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

@Serdeable
public record InventoryRequest(List<InventoryCount> counts) {}
