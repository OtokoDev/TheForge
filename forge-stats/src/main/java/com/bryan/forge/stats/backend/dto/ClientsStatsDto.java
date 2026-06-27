package com.bryan.forge.stats.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/** G — Clients : top par CA + débiteurs (factures à crédit non payées). */
@Serdeable
public record ClientsStatsDto(List<ClientStat> top, List<ClientStat> debiteurs) {

    @Serdeable
    public record ClientStat(String nom, long ca, int nbFactures, long impaye) {}
}
