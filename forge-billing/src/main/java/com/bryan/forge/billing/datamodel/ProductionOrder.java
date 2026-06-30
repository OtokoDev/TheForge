package com.bryan.forge.billing.datamodel;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Ordre de fabrication : produire {@code quantity} de {@code outputItemId}. À la clôture
 * (TERMINEE), les ingrédients de la recette sont consommés du stock et l'objet produit y entre.
 */
@Entity
@Table(name = "production_order")
public class ProductionOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "business_id", columnDefinition = "uuid", nullable = false)
    private UUID businessId;

    @Column(nullable = false)
    private long numero;

    @Column(name = "output_item_id", columnDefinition = "uuid", nullable = false)
    private UUID outputItemId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductionStatus status = ProductionStatus.EN_ATTENTE;

    /** Forgeron assigné (optionnel). */
    @Column(name = "assigned_to", columnDefinition = "uuid")
    private UUID assignedTo;

    @Column(name = "created_by", columnDefinition = "uuid", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ProductionOrder() {}

    public ProductionOrder(UUID businessId, long numero, UUID outputItemId, int quantity, UUID createdBy, UUID assignedTo) {
        this.businessId = businessId;
        this.numero = numero;
        this.outputItemId = outputItemId;
        this.quantity = quantity;
        this.createdBy = createdBy;
        this.assignedTo = assignedTo;
    }

    public UUID getId()                 { return id; }
    public UUID getBusinessId()         { return businessId; }
    public long getNumero()             { return numero; }
    public UUID getOutputItemId()       { return outputItemId; }
    public int getQuantity()            { return quantity; }
    public ProductionStatus getStatus() { return status; }
    public UUID getAssignedTo()         { return assignedTo; }
    public UUID getCreatedBy()          { return createdBy; }
    public Instant getCreatedAt()       { return createdAt; }
    public Instant getCompletedAt()     { return completedAt; }

    public void setStatus(ProductionStatus status) { this.status = status; }
    public void setAssignedTo(UUID assignedTo)     { this.assignedTo = assignedTo; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
