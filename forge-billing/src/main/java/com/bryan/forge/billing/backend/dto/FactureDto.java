package com.bryan.forge.billing.backend.dto;

import com.bryan.forge.billing.datamodel.FactureStatus;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Serdeable
public record FactureDto(
        UUID id,
        long numero,
        FactureStatus status,
        boolean paid,
        @Nullable String clientName,
        long totalAmount,
        long deposit,
        BigDecimal totalCost,
        BigDecimal totalProfit,
        BigDecimal taxRate,
        BigDecimal businessShare,
        BigDecimal workerShare,
        @Nullable String clientNote,
        @Nullable String internalNote,
        Instant createdAt,
        @Nullable Instant validatedAt,
        UUID createdBy,
        List<FactureLineDto> lines
) {}
