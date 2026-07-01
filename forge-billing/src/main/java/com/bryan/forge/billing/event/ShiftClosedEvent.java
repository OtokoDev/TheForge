package com.bryan.forge.billing.event;

import java.math.BigDecimal;
import java.time.Instant;

/** Fin de service. {@code webhookUrl} = webhook du business (null/vide → fallback URL globale). */
public record ShiftClosedEvent(
        String actorUsername,
        boolean actorWebhooksEnabled,
        String webhookUrl,
        int ordersCount,
        long totalSales,
        BigDecimal totalProfit,
        BigDecimal businessShare,
        BigDecimal workerShare,
        Instant openedAt,
        Instant closedAt
) {}
