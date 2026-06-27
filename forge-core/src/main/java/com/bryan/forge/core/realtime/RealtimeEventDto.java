package com.bryan.forge.core.realtime;

import io.micronaut.serde.annotation.Serdeable;

/** Charge diffusée aux clients WebSocket. */
@Serdeable
public record RealtimeEventDto(String businessId, String type) {
    public static RealtimeEventDto from(RealtimeEvent e) {
        return new RealtimeEventDto(e.getBusinessId() == null ? null : e.getBusinessId().toString(), e.getType());
    }
}
