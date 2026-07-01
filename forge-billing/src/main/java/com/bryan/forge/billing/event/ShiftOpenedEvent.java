package com.bryan.forge.billing.event;

import java.time.Instant;

/**
 * Prise de service. {@code actorWebhooksEnabled} = l'utilisateur reçoit-il ses webhooks.
 * {@code webhookUrl} = webhook du business (null/vide → fallback URL globale).
 */
public record ShiftOpenedEvent(
        String actorUsername,
        boolean actorWebhooksEnabled,
        String webhookUrl,
        String businessName,
        Instant openedAt
) {}
