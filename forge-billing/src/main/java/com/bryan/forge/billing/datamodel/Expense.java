package com.bryan.forge.billing.datamodel;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Dépense d'un business (septimes sortis du coffre) : matières, charges, etc. */
@Entity
@Table(name = "expense")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "business_id", columnDefinition = "uuid", nullable = false)
    private UUID businessId;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(nullable = false)
    private long amount;

    @Column(length = 60)
    private String category;

    @Column(name = "created_by", columnDefinition = "uuid", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Expense() {}

    public Expense(UUID businessId, String label, long amount, String category, UUID createdBy) {
        this.businessId = businessId;
        this.label = label;
        this.amount = amount;
        this.category = category;
        this.createdBy = createdBy;
    }

    public UUID getId()           { return id; }
    public UUID getBusinessId()   { return businessId; }
    public String getLabel()      { return label; }
    public long getAmount()       { return amount; }
    public String getCategory()   { return category; }
    public UUID getCreatedBy()    { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
