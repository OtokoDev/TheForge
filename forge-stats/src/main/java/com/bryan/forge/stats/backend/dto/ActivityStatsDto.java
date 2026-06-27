package com.bryan.forge.stats.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/** D — Activité : heatmap jour×heure (CA) + résumé sessions. dow 0=Lundi … 6=Dimanche. */
@Serdeable
public record ActivityStatsDto(
        List<HeatCell> heatmap,
        int sessions, long dureeMoyenneMin, long caParSession) {

    @Serdeable
    public record HeatCell(int dow, int hour, long ca, int count) {}
}
