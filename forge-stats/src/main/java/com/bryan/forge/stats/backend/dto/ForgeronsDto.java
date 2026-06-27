package com.bryan.forge.stats.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/** C — Forgerons : CA, bénéfice, factures, heures de service, CA/heure. */
@Serdeable
public record ForgeronsDto(List<ForgeronStat> forgerons) {

    @Serdeable
    public record ForgeronStat(String userId, String username, long ca, long benefice,
                               int nbFactures, long minutesService, double caParHeure) {}
}
