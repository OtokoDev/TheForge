package com.bryan.forge.business.backend.dto;

import com.bryan.forge.core.datamodel.GlobalRole;
import io.micronaut.serde.annotation.Serdeable;

/** Corps de PUT /api/users/{id}/role : change le rôle global (SYSTEM / STAFF / NONE). */
@Serdeable
public record SetRoleRequest(GlobalRole role) {
}
