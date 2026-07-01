package com.bryan.forge.business.backend.dto;

import com.bryan.forge.business.datamodel.MapMarkerType;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

/** {@code usageCount} = nb de marqueurs utilisant ce type (pour l'avertissement de suppression). */
@Serdeable
public record MapMarkerTypeDto(UUID id, String label, String color, @Nullable String imageDataUrl, long usageCount) {
    public static MapMarkerTypeDto from(MapMarkerType t, long usageCount) {
        return new MapMarkerTypeDto(t.getId(), t.getLabel(), t.getColor(), t.getImageDataUrl(), usageCount);
    }
}
