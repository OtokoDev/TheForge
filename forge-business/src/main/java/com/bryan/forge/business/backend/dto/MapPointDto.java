package com.bryan.forge.business.backend.dto;

import com.bryan.forge.business.datamodel.MapPoint;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

@Serdeable
public record MapPointDto(UUID id, String type, String label, int x, int y, @Nullable String note) {
    public static MapPointDto from(MapPoint p) {
        return new MapPointDto(p.getId(), p.getType(), p.getLabel(), p.getX(), p.getY(), p.getNote());
    }
}
