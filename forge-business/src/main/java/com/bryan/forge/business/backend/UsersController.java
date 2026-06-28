package com.bryan.forge.business.backend;

import com.bryan.forge.business.backend.dto.SetActiveRequest;
import com.bryan.forge.business.backend.dto.SetRoleRequest;
import com.bryan.forge.core.backend.AuditService;
import com.bryan.forge.core.backend.BannedRegistry;
import com.bryan.forge.core.backend.UserRevokedEvent;
import com.bryan.forge.core.backend.UserService;
import com.bryan.forge.core.backend.dto.UserAdminDto;
import com.bryan.forge.core.backend.dto.UserSummaryDto;
import com.bryan.forge.core.datamodel.User;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.util.List;
import java.util.UUID;

/** Recherche d'utilisateurs (autocomplétion) + administration SYSTEM (bannissement). */
@Controller("/api/users")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_AUTHENTICATED)
public class UsersController {

    private final UserService userService;
    private final CurrentUser currentUser;
    private final BannedRegistry banned;
    private final AuditService audit;
    private final ApplicationEventPublisher<UserRevokedEvent> revoked;

    public UsersController(UserService userService, CurrentUser currentUser, BannedRegistry banned,
                           AuditService audit, ApplicationEventPublisher<UserRevokedEvent> revoked) {
        this.userService = userService;
        this.currentUser = currentUser;
        this.banned = banned;
        this.audit = audit;
        this.revoked = revoked;
    }

    @Get
    public List<UserSummaryDto> search(@QueryValue(defaultValue = "") String q) {
        return userService.search(q);
    }

    /** Liste complète pour l'écran d'administration (SYSTEM). */
    @Get("/all")
    @Secured("ROLE_SYSTEM")
    public List<UserAdminDto> listAll() {
        return userService.listAll();
    }

    /**
     * Bannit (active=false) ou réactive un compte (SYSTEM). Effet immédiat : denylist en mémoire
     * (bloque l'API au prochain appel) + event de révocation (déconnecte la session active via WS).
     */
    @Put("/{id}/active")
    @Secured("ROLE_SYSTEM")
    public UserAdminDto setActive(UUID id, @Body SetActiveRequest req) {
        User actor = currentUser.require();
        if (id.equals(actor.getId()) && !req.active()) {
            throw new IllegalArgumentException("Impossible de se bannir soi-même");
        }
        User user = userService.setActive(id, req.active());
        banned.set(user.getDiscordId(), req.active());
        audit.recordSystem(actor.getId(), req.active() ? "USER_UNBAN" : "USER_BAN", user.getUsername());
        if (!req.active()) {
            revoked.publishEvent(new UserRevokedEvent(user.getDiscordId()));
        }
        return UserAdminDto.from(user);
    }

    /** Change le rôle global d'un compte (SYSTEM). Pas sur soi-même (évite l'auto-verrouillage). */
    @Put("/{id}/role")
    @Secured("ROLE_SYSTEM")
    public UserAdminDto setRole(UUID id, @Body SetRoleRequest req) {
        User actor = currentUser.require();
        if (id.equals(actor.getId())) {
            throw new IllegalArgumentException("Impossible de modifier son propre rôle");
        }
        User user = userService.setGlobalRole(id, req.role());
        audit.recordSystem(actor.getId(), "ROLE_SET", user.getUsername() + " → " + req.role());
        return UserAdminDto.from(user);
    }
}
