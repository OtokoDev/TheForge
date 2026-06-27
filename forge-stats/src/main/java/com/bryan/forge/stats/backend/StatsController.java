package com.bryan.forge.stats.backend;

import com.bryan.forge.business.backend.CurrentUser;
import com.bryan.forge.stats.backend.dto.ActivityStatsDto;
import com.bryan.forge.stats.backend.dto.ClientsStatsDto;
import com.bryan.forge.stats.backend.dto.CreancesStatsDto;
import com.bryan.forge.stats.backend.dto.ForgeronsDto;
import com.bryan.forge.stats.backend.dto.OverviewDto;
import com.bryan.forge.stats.backend.dto.ProductsDto;
import com.bryan.forge.stats.backend.dto.StockStatsDto;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Statistiques d'un business (lecture : membre/staff). Période ?from&to (ISO-8601). */
@Controller("/api/businesses/{businessId}/stats")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_AUTHENTICATED)
public class StatsController {

    private final StatsService statsService;
    private final CurrentUser currentUser;

    public StatsController(StatsService statsService, CurrentUser currentUser) {
        this.statsService = statsService;
        this.currentUser = currentUser;
    }

    @Get("/overview")
    public OverviewDto overview(UUID businessId, @Nullable @QueryValue String from, @Nullable @QueryValue String to) {
        Instant[] r = range(from, to);
        return statsService.overview(currentUser.require(), businessId, r[0], r[1]);
    }

    @Get("/products")
    public ProductsDto products(UUID businessId, @Nullable @QueryValue String from, @Nullable @QueryValue String to) {
        Instant[] r = range(from, to);
        return statsService.products(currentUser.require(), businessId, r[0], r[1]);
    }

    @Get("/forgerons")
    public ForgeronsDto forgerons(UUID businessId, @Nullable @QueryValue String from, @Nullable @QueryValue String to) {
        Instant[] r = range(from, to);
        return statsService.forgerons(currentUser.require(), businessId, r[0], r[1]);
    }

    @Get("/stock")
    public StockStatsDto stock(UUID businessId, @Nullable @QueryValue String from, @Nullable @QueryValue String to) {
        Instant[] r = range(from, to);
        return statsService.stock(currentUser.require(), businessId, r[0], r[1]);
    }

    @Get("/activity")
    public ActivityStatsDto activity(UUID businessId, @Nullable @QueryValue String from, @Nullable @QueryValue String to) {
        Instant[] r = range(from, to);
        return statsService.activity(currentUser.require(), businessId, r[0], r[1]);
    }

    @Get("/creances")
    public CreancesStatsDto creances(UUID businessId, @Nullable @QueryValue String from, @Nullable @QueryValue String to) {
        Instant[] r = range(from, to);
        return statsService.creances(currentUser.require(), businessId, r[0], r[1]);
    }

    @Get("/clients")
    public ClientsStatsDto clients(UUID businessId, @Nullable @QueryValue String from, @Nullable @QueryValue String to) {
        Instant[] r = range(from, to);
        return statsService.clients(currentUser.require(), businessId, r[0], r[1]);
    }

    /** Période : par défaut 30 derniers jours. */
    private static Instant[] range(String from, String to) {
        Instant t = to == null || to.isBlank() ? Instant.now() : Instant.parse(to);
        Instant f = from == null || from.isBlank() ? t.minus(Duration.ofDays(30)) : Instant.parse(from);
        return new Instant[]{f, t};
    }
}
