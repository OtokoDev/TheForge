package com.bryan.forge.billing.backend.dto;

import com.bryan.forge.billing.datamodel.WorkSession;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Serdeable
public record SessionDto(
        UUID id,
        Instant openedAt,
        @Nullable Instant closedAt,
        int ordersCount,
        long totalSales,
        BigDecimal totalCost,
        BigDecimal totalProfit,
        BigDecimal businessShare,
        BigDecimal workerShare
) {
    public static SessionDto from(WorkSession s) {
        return new SessionDto(s.getId(), s.getOpenedAt(), s.getClosedAt(), s.getOrdersCount(),
                s.getTotalSales(), s.getTotalCost(), s.getTotalProfit(), s.getBusinessShare(), s.getWorkerShare());
    }
}
