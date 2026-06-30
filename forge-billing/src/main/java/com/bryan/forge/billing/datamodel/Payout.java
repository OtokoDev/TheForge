package com.bryan.forge.billing.datamodel;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Versement de la part d'un forgeron (septimes sortis du coffre). */
@Entity
@Table(name = "payout")
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "business_id", columnDefinition = "uuid", nullable = false)
    private UUID businessId;

    @Column(name = "forgeron_user_id", columnDefinition = "uuid", nullable = false)
    private UUID forgeronUserId;

    @Column(nullable = false)
    private long amount;

    @Column(length = 500)
    private String note;

    @Column(name = "created_by", columnDefinition = "uuid", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Payout() {}

    public Payout(UUID businessId, UUID forgeronUserId, long amount, String note, UUID createdBy) {
        this.businessId = businessId;
        this.forgeronUserId = forgeronUserId;
        this.amount = amount;
        this.note = note;
        this.createdBy = createdBy;
    }

    public UUID getId()             { return id; }
    public UUID getBusinessId()     { return businessId; }
    public UUID getForgeronUserId() { return forgeronUserId; }
    public long getAmount()         { return amount; }
    public String getNote()         { return note; }
    public Instant getCreatedAt()   { return createdAt; }
}
