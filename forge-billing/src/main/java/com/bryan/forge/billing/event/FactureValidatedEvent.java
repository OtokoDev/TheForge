package com.bryan.forge.billing.event;

import java.math.BigDecimal;

public record FactureValidatedEvent(
        String actorUsername,
        boolean actorWebhooksEnabled,
        long numero,
        long totalAmount,
        BigDecimal totalCost,
        BigDecimal totalProfit,
        BigDecimal businessShare,
        BigDecimal workerShare
) {}
