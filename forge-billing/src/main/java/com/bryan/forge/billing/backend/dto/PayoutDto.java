package com.bryan.forge.billing.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

@Serdeable
public record PayoutDto(UUID id, UUID forgeronUserId, String forgeronName, long amount,
                        @Nullable String note, Instant createdAt) {}
