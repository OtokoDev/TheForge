package com.bryan.forge.business.backend;

import com.bryan.forge.business.backend.dto.MeDto;
import com.bryan.forge.business.backend.dto.UpdateInGameNameRequest;
import com.bryan.forge.business.backend.dto.UpdateWebhooksRequest;
import com.bryan.forge.core.backend.UserService;
import com.bryan.forge.core.backend.dto.UserDto;
import com.bryan.forge.core.datamodel.User;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Put;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

/** Profil de l'utilisateur connecté + ses appartenances (base du sélecteur de business courant). */
@Controller("/api/me")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_AUTHENTICATED)
public class MeController {

    private final CurrentUser currentUser;
    private final MembershipService membershipService;
    private final UserService userService;

    public MeController(CurrentUser currentUser, MembershipService membershipService, UserService userService) {
        this.currentUser = currentUser;
        this.membershipService = membershipService;
        this.userService = userService;
    }

    @Get
    public MeDto me() {
        return toMe(currentUser.require());
    }

    /** L'utilisateur connecté change son propre pseudo RP en jeu. */
    @Put("/in-game-name")
    public MeDto updateInGameName(@Body UpdateInGameNameRequest req) {
        User current = currentUser.require();
        User updated = userService.updateInGameName(current.getId(), req.inGameName());
        return toMe(updated);
    }

    /** L'utilisateur active/désactive la réception de ses webhooks Discord. */
    @Put("/webhooks")
    public MeDto updateWebhooks(@Body UpdateWebhooksRequest req) {
        User current = currentUser.require();
        User updated = userService.setWebhooksEnabled(current.getId(), req.enabled());
        return toMe(updated);
    }

    private MeDto toMe(User user) {
        return new MeDto(UserDto.from(user), membershipService.myMemberships(user.getId()));
    }
}
