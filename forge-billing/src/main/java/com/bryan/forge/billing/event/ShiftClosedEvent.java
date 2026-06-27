package com.bryan.forge.billing.event;

import java.math.BigDecimal;
import java.time.Instant;

public record ShiftClosedEvent(
        String actorUsername,
        boolean actorWebhooksEnabled,
        int ordersCount,
        long totalSales,
        BigDecimal totalProfit,
        BigDecimal businessShare,
        BigDecimal workerShare,
        Instant openedAt,
        Instant closedAt
) {}
