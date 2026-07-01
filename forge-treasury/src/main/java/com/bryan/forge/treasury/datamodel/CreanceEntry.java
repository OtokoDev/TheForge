package com.bryan.forge.treasury.datamodel;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Écriture immuable de créance (append-only). Reste dû d'un farmeur =
 * Σ CREDIT − Σ PAIEMENT. Montant en septimes (entier).
 */
@Entity
@Table(name = "creance_entry")
public class CreanceEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "business_id", columnDefinition = "uuid", nullable = false)
    private UUID businessId;

    /** Farmeur = nom libre (pas forcément un membre du site). */
    @Column(name = "farmer_name", nullable = false, length = 120)
    private String farmerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CreanceType type;

    @Column(nullable = false)
    private long amount;

    @Column(length = 255)
    private String reference;

    @Column(name = "created_by", columnDefinition = "uuid", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected CreanceEntry() {}

    public CreanceEntry(UUID businessId, String farmerName, CreanceType type, long amount,
                        String reference, UUID createdBy) {
        this.businessId = businessId;
        this.farmerName = farmerName;
        this.type = type;
        this.amount = amount;
        this.reference = reference;
        this.createdBy = createdBy;
    }

    public UUID getId()           { return id; }
    public UUID getBusinessId()   { return businessId; }
    public String getFarmerName()  { return farmerName; }
    public CreanceType getType()  { return type; }
    public long getAmount()       { return amount; }
    public String getReference()  { return reference; }
    public UUID getCreatedBy()    { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
