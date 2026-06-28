package com.bryan.forge.business.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

/** Corps de PUT /api/users/{id}/active : active=false → bannissement. */
@Serdeable
public record SetActiveRequest(boolean active) {
}
