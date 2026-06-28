package com.bryan.forge;

import com.bryan.forge.core.backend.BannedRegistry;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;

import java.security.Principal;

/**
 * Rend le bannissement effectif malgré le JWT stateless : bloque (401) toute requête /api/**
 * d'un compte désactivé, via la denylist en mémoire {@link BannedRegistry} (name = discordId).
 */
@ServerFilter("/api/**")
public class ActiveCheckFilter {

    private final BannedRegistry banned;

    public ActiveCheckFilter(BannedRegistry banned) {
        this.banned = banned;
    }

    @RequestFilter
    @Nullable
    public HttpResponse<?> check(HttpRequest<?> request) {
        boolean blocked = request.getUserPrincipal()
                .map(Principal::getName)
                .map(banned::isBanned)
                .orElse(false);
        return blocked ? HttpResponse.unauthorized() : null;
    }
}
