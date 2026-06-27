package com.bryan.forge.ledger.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

/** Solde courant d'un item dans un compte (projection Σ entrées − Σ sorties). */
@Serdeable
public record ItemBalanceDto(UUID itemId, String itemName, long balance) {}
