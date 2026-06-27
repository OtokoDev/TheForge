package com.bryan.forge.ledger.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

/** Une ligne de stock = solde d'un item dans un compte (projection), tous comptes confondus. */
@Serdeable
public record StockRowDto(UUID accountId, String accountName, UUID itemId, String itemName, long quantity) {}
