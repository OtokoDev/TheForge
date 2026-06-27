package com.bryan.forge.core.datamodel;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Entrée de la main courante (journal d'activité). Append-only. */
@Entity
@Table(name = "activity_log")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "business_id", columnDefinition = "uuid")
    private UUID businessId;

    /** Auteur ; null pour un événement sans utilisateur connu (ex. login échoué). */
    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 500)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ActivityLog() {}

    public ActivityLog(UUID businessId, UUID userId, String action, String details) {
        this.businessId = businessId;
        this.userId = userId;
        this.action = action;
        this.details = details;
    }

    public UUID getId()           { return id; }
    public UUID getBusinessId()   { return businessId; }
    public UUID getUserId()       { return userId; }
    public String getAction()     { return action; }
    public String getDetails()    { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
