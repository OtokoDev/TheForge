package com.bryan.forge.security.backend;

import com.bryan.forge.core.datamodel.SystemActors;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.core.datarepository.UserRepository;
import io.micronaut.security.utils.SecurityService;
import jakarta.inject.Singleton;

import java.util.UUID;

/**
 * Identité courante pour la traçabilité (created_by / modified_by). S'appuie sur
 * {@link SecurityService}, dont le contexte est propagé par Micronaut à travers les threads
 * {@code @ExecuteOn(BLOCKING)} : l'user de la requête est donc bien vu côté service. Hors
 * requête (CRON, thread manuel, async) → pas d'authentification → {@link SystemActors#SYSTEM_ID}.
 */
@Singleton
public class CurrentActor {

    private final SecurityService security;
    private final UserRepository userRepo;

    public CurrentActor(SecurityService security, UserRepository userRepo) {
        this.security = security;
        this.userRepo = userRepo;
    }

    /** Id de l'utilisateur courant, ou {@code SYSTEM_ID} s'il n'y a pas de requête authentifiée. */
    public UUID stampId() {
        return security.getAuthentication()
                .flatMap(a -> userRepo.findByDiscordId(a.getName()))
                .map(User::getId)
                .orElse(SystemActors.SYSTEM_ID);
    }
}
