package com.bryan.forge.backend.realtime;

import com.bryan.forge.core.realtime.RealtimeEvent;
import com.bryan.forge.core.realtime.RealtimeEventDto;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.transaction.annotation.TransactionalEventListener;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import jakarta.inject.Singleton;

/**
 * Diffuse les {@link RealtimeEvent} métier APRÈS commit de leur transaction (zéro event
 * fantôme en cas de rollback), aux seules sessions abonnées au business concerné.
 */
@Singleton
public class RealtimeDispatcher {

    private final WebSocketBroadcaster broadcaster;   // null si le serveur ne supporte pas les WS

    public RealtimeDispatcher(@Nullable WebSocketBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @TransactionalEventListener
    public void onEvent(RealtimeEvent event) {
        if (broadcaster == null || event.getBusinessId() == null) return;
        broadcaster.broadcastSync(RealtimeEventDto.from(event), session -> matches(session, event));
    }

    private boolean matches(WebSocketSession session, RealtimeEvent event) {
        return session.get("business", String.class)
                .map(b -> b.equals(event.getBusinessId().toString()))
                .orElse(false);
    }
}
