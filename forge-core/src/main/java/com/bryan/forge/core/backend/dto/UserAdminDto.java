package com.bryan.forge.core.backend.dto;

import com.bryan.forge.core.datamodel.GlobalRole;
import com.bryan.forge.core.datamodel.User;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

/** Vue admin d'un utilisateur (gestion des rôles / bannissement, SYSTEM). */
@Serdeable
public record UserAdminDto(UUID id, String username, String discordId, String inGameName,
                           GlobalRole globalRole, boolean active) {
    public static UserAdminDto from(User u) {
        return new UserAdminDto(u.getId(), u.getUsername(), u.getDiscordId(), u.getInGameName(),
                u.getGlobalRole(), u.isActive());
    }
}
