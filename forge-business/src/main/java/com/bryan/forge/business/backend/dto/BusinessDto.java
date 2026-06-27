package com.bryan.forge.business.backend.dto;

import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datamodel.BusinessType;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

@Serdeable
public record BusinessDto(UUID id, String nom, BusinessType type, Instant createdAt) {
    public static BusinessDto from(Business b) {
        return new BusinessDto(b.getId(), b.getNom(), b.getType(), b.getCreatedAt());
    }
}
