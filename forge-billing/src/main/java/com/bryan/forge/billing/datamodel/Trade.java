package com.bryan.forge.billing.datamodel;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Échange inter-business : le vendeur (from) propose des items contre des septims payés
 * par l'acheteur (to). À l'acceptation, marchandise et septims bougent atomiquement.
 */
@Entity
@Table(name = "trade")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private long numero;

    @Column(name = "from_business_id", columnDefinition = "uuid", nullable = false)
    private UUID fromBusinessId;

    @Column(name = "to_business_id", columnDefinition = "uuid", nullable = false)
    private UUID toBusinessId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TradeStatus status = TradeStatus.PROPOSEE;

    /** Contrepartie en septims payée par l'acheteur (0 = don). */
    @Column(nullable = false)
    private long septims = 0;

    @Column(length = 500)
    private String note;

    @Column(name = "created_by", columnDefinition = "uuid", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "decided_by", columnDefinition = "uuid")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected Trade() {}

    public Trade(long numero, UUID fromBusinessId, UUID toBusinessId, long septims, String note, UUID createdBy) {
        this.numero = numero;
        this.fromBusinessId = fromBusinessId;
        this.toBusinessId = toBusinessId;
        this.septims = septims;
        this.note = note;
        this.createdBy = createdBy;
    }

    public UUID getId()             { return id; }
    public long getNumero()         { return numero; }
    public UUID getFromBusinessId() { return fromBusinessId; }
    public UUID getToBusinessId()   { return toBusinessId; }
    public TradeStatus getStatus()  { return status; }
    public long getSeptims()        { return septims; }
    public String getNote()         { return note; }
    public UUID getCreatedBy()      { return createdBy; }
    public Instant getCreatedAt()   { return createdAt; }
    public UUID getDecidedBy()      { return decidedBy; }
    public Instant getDecidedAt()   { return decidedAt; }

    public void decide(TradeStatus status, UUID decidedBy) {
        this.status = status;
        this.decidedBy = decidedBy;
        this.decidedAt = Instant.now();
    }
}
