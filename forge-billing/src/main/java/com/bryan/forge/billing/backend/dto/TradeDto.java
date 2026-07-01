package com.bryan.forge.billing.backend.dto;

import com.bryan.forge.billing.datamodel.TradeStatus;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Serdeable
public record TradeDto(
        UUID id,
        long numero,
        UUID fromBusinessId,
        String fromBusinessName,
        UUID toBusinessId,
        String toBusinessName,
        TradeStatus status,
        long septims,
        @Nullable String note,
        Instant createdAt,
        List<TradeLineDto> lines
) {
    @Serdeable
    public record TradeLineDto(UUID itemId, String itemName, int quantity) {}
}
