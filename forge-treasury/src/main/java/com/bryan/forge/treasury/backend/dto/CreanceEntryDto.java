package com.bryan.forge.treasury.backend.dto;

import com.bryan.forge.treasury.datamodel.CreanceType;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;

@Serdeable
public record CreanceEntryDto(CreanceType type, long amount, @Nullable String reference, String username, Instant createdAt) {}
