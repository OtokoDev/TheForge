package com.bryan.forge.security.backend;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.client.annotation.Client;

/**
 * Client déclaratif vers l'API Discord. Sert à récupérer le profil de l'utilisateur
 * juste après l'échange du code OAuth contre un access token.
 */
@Client("https://discord.com/api/v10")
@Header(name = "User-Agent", value = "ForgeRP (https://github.com/bryan/forge, 0.1.0)")
public interface DiscordApiClient {

    /** {@code authorization} doit valoir « Bearer &lt;access_token&gt; ». */
    @Get("/users/@me")
    DiscordUser me(@Header("Authorization") String authorization);
}
