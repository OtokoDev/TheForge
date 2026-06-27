package com.bryan.forge.security.backend;

import com.bryan.forge.core.backend.AuditService;
import com.bryan.forge.core.backend.UserService;
import com.bryan.forge.core.datamodel.GlobalRole;
import com.bryan.forge.core.datamodel.User;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.oauth2.endpoint.authorization.state.State;
import io.micronaut.security.oauth2.endpoint.token.response.OauthAuthenticationMapper;
import io.micronaut.security.oauth2.endpoint.token.response.TokenResponse;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/**
 * Pont OAuth2 Discord → identité Forge. Après l'échange du code contre un access
 * token, Micronaut nous remet le {@link TokenResponse} : on récupère le profil
 * Discord, on upsert le {@link User}, et on émet une réponse d'authentification
 * portant les rôles globaux (qui finiront dans le JWT).
 *
 * Le travail (appel HTTP Discord bloquant + upsert JPA) est exécuté sur un thread
 * worker via {@code subscribeOn(boundedElastic())} : ce mapper est invoqué sur
 * l'event-loop Netty, qui ne doit jamais être bloqué.
 *
 * Le nom du bean ({@code @Named("discord")}) DOIT correspondre au nom du client
 * configuré sous {@code micronaut.security.oauth2.clients.discord.*}.
 */
@Named("discord")
@Singleton
public class DiscordAuthenticationMapper implements OauthAuthenticationMapper {

    private static final Logger WEB = LoggerFactory.getLogger("com.bryan.forge.web");

    private final DiscordApiClient discord;
    private final UserService userService;
    private final AuditService auditService;
    private final String ownerDiscordId;

    public DiscordAuthenticationMapper(DiscordApiClient discord,
                                       UserService userService,
                                       AuditService auditService,
                                       @Value("${forge.owner-discord-id:}") String ownerDiscordId) {
        this.discord = discord;
        this.userService = userService;
        this.auditService = auditService;
        this.ownerDiscordId = ownerDiscordId;
    }

    @Override
    public Publisher<AuthenticationResponse> createAuthenticationResponse(TokenResponse tokenResponse,
                                                                          @Nullable State state) {
        return Mono.fromCallable(() -> authenticate(tokenResponse))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private AuthenticationResponse authenticate(TokenResponse tokenResponse) {
        DiscordUser profile = discord.me("Bearer " + tokenResponse.getAccessToken());

        String avatarUrl = profile.avatar() == null
                ? null
                : "https://cdn.discordapp.com/avatars/" + profile.id() + "/" + profile.avatar() + ".png";

        User user = userService.upsertFromDiscord(profile.id(), profile.username(), avatarUrl);

        // Le « big boss » configuré est promu SYSTEM à sa première connexion.
        if (ownerDiscordId != null && !ownerDiscordId.isBlank()
                && ownerDiscordId.equals(user.getDiscordId())
                && user.getGlobalRole() == GlobalRole.NONE) {
            user = userService.setGlobalRole(user.getId(), GlobalRole.SYSTEM);
        }

        if (!user.isActive()) {
            auditService.recordSystem(user.getId(), "LOGIN_FAIL", user.getUsername() + " — compte désactivé");
            WEB.warn("Login refusé : {} (compte désactivé)", user.getUsername());
            return AuthenticationResponse.failure("Compte désactivé");
        }

        auditService.recordSystem(user.getId(), "LOGIN_OK", user.getUsername());
        WEB.info("Login OK : {}", user.getUsername());
        return AuthenticationResponse.success(
                user.getDiscordId(),
                rolesFor(user.getGlobalRole()),
                Map.of("username", user.getUsername()));
    }

    /** Convertit le rôle global en autorités JWT consommées par {@code @Secured}. */
    static List<String> rolesFor(GlobalRole role) {
        return switch (role) {
            case SYSTEM -> List.of("ROLE_SYSTEM");
            case STAFF -> List.of("ROLE_STAFF");
            case NONE -> List.of();
        };
    }
}
