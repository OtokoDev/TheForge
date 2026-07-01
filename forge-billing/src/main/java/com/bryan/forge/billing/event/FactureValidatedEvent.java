package com.bryan.forge.billing.event;

import java.math.BigDecimal;

/** Facture validée. {@code webhookUrl} = webhook du business (null/vide → fallback URL globale). */
public record FactureValidatedEvent(
        String actorUsername,
        boolean actorWebhooksEnabled,
        String webhookUrl,
        long numero,
        long totalAmount,
        BigDecimal totalCost,
        BigDecimal totalProfit,
        BigDecimal businessShare,
        BigDecimal workerShare
) {}
