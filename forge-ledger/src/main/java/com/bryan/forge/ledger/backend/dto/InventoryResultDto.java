package com.bryan.forge.ledger.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

/** Résultat d'une validation d'inventaire : nombre de lignes régularisées. */
@Serdeable
public record InventoryResultDto(int adjusted) {}
