package com.bryan.forge.ledger.backend.dto;

import com.bryan.forge.ledger.datamodel.MovementType;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

@Serdeable
public record RecordMovementRequest(
        UUID itemId,
        int quantity,
        @Nullable UUID fromAccountId,
        @Nullable UUID toAccountId,
        MovementType type,
        @Nullable String note
) {}
