package com.bryan.forge.security.backend;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Sous-ensemble de la réponse Discord {@code GET /users/@me} qui nous intéresse.
 * {@code id} est le snowflake stable ; {@code avatar} est un hash (peut être null).
 */
@Serdeable
public record DiscordUser(
        String id,
        String username,
        @Nullable String avatar
) {}
