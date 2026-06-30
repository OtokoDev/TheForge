package com.bryan.forge.billing.backend.dto;

import com.bryan.forge.billing.datamodel.CommandeStatus;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Serdeable
public record CommandeDto(
        UUID id,
        long numero,
        CommandeStatus status,
        @Nullable String clientName,
        @Nullable String clientNote,
        @Nullable Instant dueDate,
        long acompte,
        @Nullable UUID factureId,
        long total,
        UUID createdBy,
        Instant createdAt,
        List<CommandeLineDto> lines
) {}
