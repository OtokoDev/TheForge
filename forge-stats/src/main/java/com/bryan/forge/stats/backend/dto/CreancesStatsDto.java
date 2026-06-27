package com.bryan.forge.stats.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/** F — Créances : totaux courants + série crédit/paiement sur la période + top farmeurs. */
@Serdeable
public record CreancesStatsDto(
        long totalDu, long totalCredit, long totalPaid, double ratioPaye,
        List<DayCreance> serie,
        List<FarmerStat> topFarmers) {

    @Serdeable
    public record DayCreance(String jour, long credit, long paiement) {}

    @Serdeable
    public record FarmerStat(String username, long credited, long paid, long remaining) {}
}
