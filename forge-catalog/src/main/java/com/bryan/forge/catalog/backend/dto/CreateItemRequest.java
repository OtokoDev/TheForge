package com.bryan.forge.catalog.backend.dto;

import com.bryan.forge.catalog.datamodel.HandRequired;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

@Serdeable
public record CreateItemRequest(String name, @Nullable UUID familyId, @Nullable UUID materialId,
                                @Nullable HandRequired handRequired) {}
