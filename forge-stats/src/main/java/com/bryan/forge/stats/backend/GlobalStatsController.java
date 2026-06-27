package com.bryan.forge.stats.backend;

import com.bryan.forge.stats.backend.dto.GlobalStatsDto;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;

import java.time.Duration;
import java.time.Instant;

/** Stats globales (tous business) — réservé aux rôles globaux STAFF / SYSTEM. */
@Controller("/api/staff/stats")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured({"ROLE_STAFF", "ROLE_SYSTEM"})
public class GlobalStatsController {

    private final GlobalStatsService service;

    public GlobalStatsController(GlobalStatsService service) {
        this.service = service;
    }

    @Get("/overview")
    public GlobalStatsDto overview(@Nullable @QueryValue String from, @Nullable @QueryValue String to) {
        Instant t = to == null || to.isBlank() ? Instant.now() : Instant.parse(to);
        Instant f = from == null || from.isBlank() ? t.minus(Duration.ofDays(84)) : Instant.parse(from);
        return service.overview(f, t);
    }
}
