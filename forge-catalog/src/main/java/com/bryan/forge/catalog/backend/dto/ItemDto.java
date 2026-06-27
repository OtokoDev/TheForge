package com.bryan.forge.catalog.backend.dto;

import com.bryan.forge.catalog.datamodel.HandRequired;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

@Serdeable
public record ItemDto(
        UUID id,
        String name,
        @Nullable UUID familyId,
        @Nullable String familyName,
        @Nullable String familyColor,
        @Nullable UUID materialId,
        @Nullable String materialName,
        @Nullable String materialColor,
        @Nullable HandRequired handRequired,
        boolean active,
        boolean system,
        boolean hasRecipe,
        int version
) {}
