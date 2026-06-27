package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.SessionDto;
import com.bryan.forge.billing.backend.dto.ShiftStatusDto;
import com.bryan.forge.business.backend.CurrentUser;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.util.List;
import java.util.UUID;

/** Prises de poste d'un business. */
@Controller("/api/businesses/{businessId}/sessions")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_AUTHENTICATED)
public class SessionController {

    private final SessionService sessionService;
    private final CurrentUser currentUser;

    public SessionController(SessionService sessionService, CurrentUser currentUser) {
        this.sessionService = sessionService;
        this.currentUser = currentUser;
    }

    @Get("/current")
    public ShiftStatusDto current(UUID businessId) {
        return sessionService.current(currentUser.require(), businessId);
    }

    @Post("/open")
    public SessionDto open(UUID businessId) {
        return sessionService.open(currentUser.require(), businessId);
    }

    @Post("/close")
    public SessionDto close(UUID businessId) {
        return sessionService.close(currentUser.require(), businessId);
    }

    @Get
    public List<SessionDto> history(UUID businessId) {
        return sessionService.history(currentUser.require(), businessId);
    }
}
