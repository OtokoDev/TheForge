package com.bryan.forge.ledger.datamodel;

import jakarta.persistence.*;

import java.util.UUID;

/** Seuil d'alerte de rupture par item et par business (stock total < minQty → alerte). */
@Entity
@Table(name = "stock_threshold", uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "item_id"}))
public class StockThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "business_id", columnDefinition = "uuid", nullable = false)
    private UUID businessId;

    @Column(name = "item_id", columnDefinition = "uuid", nullable = false)
    private UUID itemId;

    @Column(name = "min_qty", nullable = false)
    private int minQty;

    protected StockThreshold() {}

    public StockThreshold(UUID businessId, UUID itemId, int minQty) {
        this.businessId = businessId;
        this.itemId = itemId;
        this.minQty = minQty;
    }

    public UUID getId()         { return id; }
    public UUID getBusinessId() { return businessId; }
    public UUID getItemId()     { return itemId; }
    public int getMinQty()    { return minQty; }
    public void setMinQty(int minQty) { this.minQty = minQty; }
}
