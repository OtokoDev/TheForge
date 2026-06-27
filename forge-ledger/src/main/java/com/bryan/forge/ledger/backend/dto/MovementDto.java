package com.bryan.forge.ledger.backend.dto;

import com.bryan.forge.ledger.datamodel.MovementType;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

@Serdeable
public record MovementDto(
        UUID id,
        UUID itemId,
        String itemName,
        int quantity,
        @Nullable UUID fromAccountId,
        @Nullable String fromAccountName,
        @Nullable UUID toAccountId,
        @Nullable String toAccountName,
        MovementType type,
        @Nullable String note,
        Instant createdAt
) {}
