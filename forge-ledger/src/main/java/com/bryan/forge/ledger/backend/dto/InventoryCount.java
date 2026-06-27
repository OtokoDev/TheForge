package com.bryan.forge.ledger.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

/** Quantité comptée en jeu pour un item d'un compte (mode inventaire). */
@Serdeable
public record InventoryCount(UUID accountId, UUID itemId, long counted) {}
