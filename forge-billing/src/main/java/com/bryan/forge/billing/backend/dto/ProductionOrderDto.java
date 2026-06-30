package com.bryan.forge.billing.backend.dto;

import com.bryan.forge.billing.datamodel.ProductionStatus;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

@Serdeable
public record ProductionOrderDto(
        UUID id,
        long numero,
        UUID outputItemId,
        String outputItemName,
        int quantity,
        ProductionStatus status,
        @Nullable UUID assignedTo,
        Instant createdAt,
        @Nullable Instant completedAt
) {}
