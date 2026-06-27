package com.bryan.forge.catalog.backend.dto;

import com.bryan.forge.catalog.datamodel.Taxon;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

@Serdeable
public record TaxonDto(UUID id, String nom, int ordre, @Nullable String couleur, int version) {
    public static TaxonDto from(Taxon t) {
        return new TaxonDto(t.getId(), t.getNom(), t.getOrdre(), t.getCouleur(), t.getVersion());
    }
}
