package com.bryan.forge.stats.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/** A — CA & marge. Valeurs courantes + précédentes (le front calcule les Δ). Septims = long. */
@Serdeable
public record OverviewDto(
        long caEncaisse, long caEncaissePrev,
        long benefice, long beneficePrev,
        int nbFactures, int nbFacturesPrev,
        long panierMoyen, long panierMoyenPrev,
        double tauxMarge,
        long impaye, int impayeCount,
        long partBusiness, long partForgeron,
        List<DayPoint> serie) {

    @Serdeable
    public record DayPoint(String jour, long ca, long benefice) {}
}
