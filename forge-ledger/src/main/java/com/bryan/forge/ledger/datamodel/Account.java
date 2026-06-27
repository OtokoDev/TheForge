package com.bryan.forge.ledger.datamodel;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Emplacement détenant des items (un coffre, un stock…). Plusieurs comptes par business. */
@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "business_id", columnDefinition = "uuid", nullable = false)
    private UUID businessId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountKind kind;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Account() {}

    public Account(UUID businessId, String name, AccountKind kind) {
        this.businessId = businessId;
        this.name = name;
        this.kind = kind;
    }

    public UUID getId()           { return id; }
    public UUID getBusinessId()   { return businessId; }
    public String getName()       { return name; }
    public AccountKind getKind()  { return kind; }
    public Instant getCreatedAt() { return createdAt; }
}
