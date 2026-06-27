package com.bryan.forge.stats.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/** Vue STAFF (globale, tous business) : CA/semaine, ventilation par business, items les plus vendus. */
@Serdeable
public record GlobalStatsDto(
        long totalCa, long totalBenefice,
        List<WeekPoint> serie,
        List<NameValue> parBusiness,
        List<NameValue> topItems) {

    @Serdeable
    public record WeekPoint(String semaine, long ca, long benefice) {}
}
