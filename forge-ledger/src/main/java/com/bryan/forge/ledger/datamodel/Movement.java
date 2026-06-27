package com.bryan.forge.ledger.datamodel;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Écriture immuable du journal (append-only). {@code quantity} unités de {@code item}
 * vont de {@code fromAccount} vers {@code toAccount}. from nul = création, to nul =
 * destruction, les deux = transfert. Jamais modifié ni supprimé.
 */
@Entity
@Table(name = "movement")
public class Movement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "business_id", columnDefinition = "uuid", nullable = false)
    private UUID businessId;

    @Column(name = "item_id", columnDefinition = "uuid", nullable = false)
    private UUID itemId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "from_account_id", columnDefinition = "uuid")
    private UUID fromAccountId;

    @Column(name = "to_account_id", columnDefinition = "uuid")
    private UUID toAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType type;

    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(name = "reference_id", columnDefinition = "uuid")
    private UUID referenceId;

    @Column(length = 255)
    private String note;

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Movement() {}

    public Movement(UUID businessId, UUID itemId, int quantity, UUID fromAccountId, UUID toAccountId,
                    MovementType type, String referenceType, UUID referenceId, String note, UUID userId) {
        this.businessId = businessId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.type = type;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.note = note;
        this.userId = userId;
    }

    public UUID getId()            { return id; }
    public UUID getBusinessId()    { return businessId; }
    public UUID getItemId()        { return itemId; }
    public int getQuantity()       { return quantity; }
    public UUID getFromAccountId() { return fromAccountId; }
    public UUID getToAccountId()   { return toAccountId; }
    public MovementType getType()  { return type; }
    public String getReferenceType() { return referenceType; }
    public UUID getReferenceId()   { return referenceId; }
    public String getNote()        { return note; }
    public UUID getUserId()        { return userId; }
    public Instant getCreatedAt()  { return createdAt; }
}
