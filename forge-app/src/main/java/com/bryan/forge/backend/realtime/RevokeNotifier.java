package com.bryan.forge.backend.realtime;

import com.bryan.forge.core.backend.UserRevokedEvent;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.websocket.WebSocketBroadcaster;
import jakarta.inject.Singleton;

import java.util.Map;

/**
 * Au bannissement : pousse un message {@code {"type":"REVOKED"}} aux sessions WS du compte visé
 * (clé {@code username} = discordId stockée à l'open) → le front déconnecte la session active.
 */
@Singleton
public class RevokeNotifier {

    private final WebSocketBroadcaster broadcaster;

    public RevokeNotifier(@Nullable WebSocketBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @EventListener
    public void onRevoked(UserRevokedEvent event) {
        if (broadcaster == null) {
            return;
        }
        broadcaster.broadcastSync(Map.of("type", "REVOKED"),
                session -> session.get("username", String.class)
                        .map(u -> u.equals(event.discordId()))
                        .orElse(false));
    }
}
