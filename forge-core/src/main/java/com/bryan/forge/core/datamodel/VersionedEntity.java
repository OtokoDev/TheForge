package com.bryan.forge.core.datamodel;

import io.micronaut.data.annotation.DateUpdated;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Base des entités mutables : verrou optimiste ({@code version}) + traçabilité (qui/quand).
 * Le {@code version} est renvoyé au front et comparé à l'update : s'il diffère, l'entité a
 * été modifiée entre-temps → erreur métier « données modifiées, actualiser » (HTTP 409).
 * Hibernate incrémente {@code version} à chaque update (garde-fou anti lost-update).
 *
 * Les journaux append-only (Movement, CreanceEntry, ActivityLog) n'héritent PAS de cette
 * classe : ils sont immuables.
 */
@MappedSuperclass
public abstract class VersionedEntity {

    @Version
    @Column(nullable = false)
    private int version;

    @DateUpdated
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

    @Column(name = "modified_by", columnDefinition = "uuid")
    private UUID modifiedBy;

    public int getVersion()       { return version; }
    public void setVersion(int v) { this.version = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy()    { return createdBy; }
    public UUID getModifiedBy()   { return modifiedBy; }

    public void setCreatedBy(UUID createdBy)   { this.createdBy = createdBy; }
    public void setModifiedBy(UUID modifiedBy) { this.modifiedBy = modifiedBy; }
}
