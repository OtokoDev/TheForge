package com.bryan.forge.ledger.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

/** Item dont le stock total est sous le seuil d'alerte. */
@Serdeable
public record ThresholdAlertDto(UUID itemId, String itemName, long stock, int minQty) {}
