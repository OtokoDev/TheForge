package com.bryan.forge.ledger.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

/** Comptes par défaut du business (POS). */
@Serdeable
public record DefaultsDto(@Nullable UUID stockAccountId, @Nullable UUID coffreAccountId) {}
