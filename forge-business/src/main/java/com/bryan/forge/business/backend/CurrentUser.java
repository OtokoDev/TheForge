package com.bryan.forge.business.backend;

import com.bryan.forge.core.backend.ForbiddenException;
import com.bryan.forge.core.backend.UserService;
import com.bryan.forge.core.datamodel.User;
import io.micronaut.security.utils.SecurityService;
import jakarta.inject.Singleton;

/**
 * Résout l'utilisateur courant à partir du JWT. Le « name » du principal est le
 * {@code discordId} (cf. {@code DiscordAuthenticationMapper}).
 */
@Singleton
public class CurrentUser {

    private final SecurityService security;
    private final UserService userService;

    public CurrentUser(SecurityService security, UserService userService) {
        this.security = security;
        this.userService = userService;
    }

    public User require() {
        String discordId = security.username()
                .orElseThrow(() -> new ForbiddenException("Non authentifié"));
        return userService.requireByDiscordId(discordId);
    }
}
