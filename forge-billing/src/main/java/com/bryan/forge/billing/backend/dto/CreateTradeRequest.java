package com.bryan.forge.billing.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;
import java.util.UUID;

/** Proposition d'échange : items du vendeur contre {@code septims} payés par l'acheteur. */
@Serdeable
public record CreateTradeRequest(UUID toBusinessId, long septims, @Nullable String note, List<Line> lines) {
    @Serdeable
    public record Line(UUID itemId, int quantity) {}
}
