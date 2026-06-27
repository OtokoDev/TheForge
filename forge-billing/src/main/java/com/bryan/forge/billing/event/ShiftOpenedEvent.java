package com.bryan.forge.billing.event;

import java.time.Instant;

/** Prise de service. {@code actorWebhooksEnabled} = l'utilisateur reçoit-il ses webhooks. */
public record ShiftOpenedEvent(
        String actorUsername,
        boolean actorWebhooksEnabled,
        String businessName,
        Instant openedAt
) {}
