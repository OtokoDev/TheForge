package com.bryan.forge.core.backend;

/** Émis au bannissement d'un utilisateur → le front de la session active est déconnecté (WS). */
public record UserRevokedEvent(String discordId) {
}
