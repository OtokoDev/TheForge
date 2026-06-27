package com.bryan.forge.business.backend;

import com.bryan.forge.core.backend.UserService;
import com.bryan.forge.core.backend.dto.UserSummaryDto;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.util.List;

/** Recherche d'utilisateurs pour l'autocomplétion (ajout de membre à un business). */
@Controller("/api/users")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_AUTHENTICATED)
public class UsersController {

    private final UserService userService;

    public UsersController(UserService userService) {
        this.userService = userService;
    }

    @Get
    public List<UserSummaryDto> search(@QueryValue(defaultValue = "") String q) {
        return userService.search(q);
    }
}
