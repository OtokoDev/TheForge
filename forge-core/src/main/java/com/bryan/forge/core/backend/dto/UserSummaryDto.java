package com.bryan.forge.core.backend.dto;

import com.bryan.forge.core.datamodel.User;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

/** Vue minimale pour l'autocomplétion (ajout de membre à un business). */
@Serdeable
public record UserSummaryDto(UUID id, String username, @Nullable String inGameName) {
    public static UserSummaryDto from(User u) {
        return new UserSummaryDto(u.getId(), u.getUsername(), u.getInGameName());
    }
}
