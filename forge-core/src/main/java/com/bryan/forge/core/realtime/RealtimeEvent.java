package com.bryan.forge.core.realtime;

import java.util.UUID;

/**
 * Événement de domaine à diffuser en temps réel (WebSocket), scopé à un business.
 * Publié par les services via {@code ApplicationEventPublisher} ; diffusé après commit
 * par le RealtimeDispatcher aux sessions abonnées à ce business.
 */
public class RealtimeEvent {

    private final UUID businessId;
    private final String type;   // ex. "STOCK", "FACTURE", "CREANCE", "CATALOGUE"

    public RealtimeEvent(UUID businessId, String type) {
        this.businessId = businessId;
        this.type = type;
    }

    public UUID getBusinessId() { return businessId; }
    public String getType()     { return type; }
}
