package com.bryan.forge.ledger.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

@Serdeable
public record SetDefaultsRequest(@Nullable UUID stockAccountId, @Nullable UUID coffreAccountId) {}
