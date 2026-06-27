package com.bryan.forge.catalog.backend.dto;

import com.bryan.forge.catalog.datamodel.RecipeComponent;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

@Serdeable
public record RecipeComponentDto(UUID componentItemId, String componentName, int quantity) {
    public static RecipeComponentDto from(RecipeComponent rc) {
        return new RecipeComponentDto(rc.getComponentItem().getId(), rc.getComponentItem().getName(), rc.getQuantity());
    }
}
