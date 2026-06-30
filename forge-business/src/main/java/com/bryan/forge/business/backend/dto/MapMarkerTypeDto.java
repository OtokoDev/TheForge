package com.bryan.forge.business.backend.dto;

import com.bryan.forge.business.datamodel.MapMarkerType;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

@Serdeable
public record MapMarkerTypeDto(UUID id, String label, String color, @Nullable String imageDataUrl) {
    public static MapMarkerTypeDto from(MapMarkerType t) {
        return new MapMarkerTypeDto(t.getId(), t.getLabel(), t.getColor(), t.getImageDataUrl());
    }
}
