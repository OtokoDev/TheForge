package com.bryan.forge.business.backend.dto;

import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datamodel.BusinessType;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Serdeable
public record BusinessDto(UUID id, String nom, BusinessType type, List<String> hiddenScreens, Instant createdAt) {
    public static BusinessDto from(Business b) {
        return new BusinessDto(b.getId(), b.getNom(), b.getType(), parse(b.getHiddenScreens()), b.getCreatedAt());
    }

    private static List<String> parse(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
