package com.bryan.forge.billing.datamodel;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "purchase_line")
public class PurchaseLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "purchase_id", columnDefinition = "uuid", nullable = false)
    private UUID purchaseId;

    @Column(name = "item_id", columnDefinition = "uuid", nullable = false)
    private UUID itemId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    protected PurchaseLine() {}

    public PurchaseLine(UUID purchaseId, UUID itemId, int quantity, BigDecimal unitCost) {
        this.purchaseId = purchaseId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    public UUID getId()         { return id; }
    public UUID getPurchaseId() { return purchaseId; }
    public UUID getItemId()     { return itemId; }
    public int getQuantity()    { return quantity; }
    public BigDecimal getUnitCost() { return unitCost; }
}
