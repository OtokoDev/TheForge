package com.bryan.forge.core.backend.dto;

import com.bryan.forge.core.datamodel.GlobalRole;
import com.bryan.forge.core.datamodel.User;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

@Serdeable
public record UserDto(
        UUID id,
        String discordId,
        String username,
        @Nullable String inGameName,
        @Nullable String avatar,
        GlobalRole globalRole,
        boolean active,
        boolean webhooksEnabled,
        Instant createdAt
) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getDiscordId(), u.getUsername(), u.getInGameName(),
                u.getAvatar(), u.getGlobalRole(), u.isActive(), u.isWebhooksEnabled(), u.getCreatedAt());
    }
}
