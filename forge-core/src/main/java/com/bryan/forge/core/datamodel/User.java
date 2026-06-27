package com.bryan.forge.core.datamodel;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Compte utilisateur de la plateforme. L'identité provient de Discord (OAuth2) :
 * {@code discordId} est la clé stable, le reste est rafraîchi à chaque connexion.
 */
@Entity
@Table(name = "app_user")
@Serdeable
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "discord_id", unique = true, nullable = false, length = 32)
    private String discordId;

    @Column(nullable = false, length = 100)
    private String username;

    /** Nom RP en jeu, saisi par l'utilisateur (optionnel). */
    @Column(name = "in_game_name", length = 100)
    private String inGameName;

    @Column(length = 255)
    private String avatar;

    @Enumerated(EnumType.STRING)
    @Column(name = "global_role", nullable = false, length = 20)
    private GlobalRole globalRole = GlobalRole.NONE;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** Réception des webhooks Discord pour les actions de cet utilisateur. */
    @Column(name = "webhooks_enabled", nullable = false)
    private boolean webhooksEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected User() {}

    public User(String discordId, String username, String avatar) {
        this.discordId = discordId;
        this.username = username;
        this.avatar = avatar;
    }

    public UUID getId()            { return id; }
    public String getDiscordId()   { return discordId; }
    public String getUsername()    { return username; }
    public String getInGameName()  { return inGameName; }
    public String getAvatar()      { return avatar; }
    public GlobalRole getGlobalRole() { return globalRole; }
    public boolean isActive()      { return active; }
    public boolean isWebhooksEnabled() { return webhooksEnabled; }
    public Instant getCreatedAt()  { return createdAt; }

    public void setUsername(String username)        { this.username = username; }
    public void setInGameName(String inGameName)    { this.inGameName = inGameName; }
    public void setAvatar(String avatar)            { this.avatar = avatar; }
    public void setGlobalRole(GlobalRole globalRole) { this.globalRole = globalRole; }
    public void setActive(boolean active)           { this.active = active; }
    public void setWebhooksEnabled(boolean webhooksEnabled) { this.webhooksEnabled = webhooksEnabled; }
}
