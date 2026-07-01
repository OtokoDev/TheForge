package com.bryan.forge.billing.datamodel;

import jakarta.persistence.*;

import java.util.UUID;

/** Ligne d'un échange inter-business : item + quantité. */
@Entity
@Table(name = "trade_line")
public class TradeLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "trade_id", columnDefinition = "uuid", nullable = false)
    private UUID tradeId;

    @Column(name = "item_id", columnDefinition = "uuid", nullable = false)
    private UUID itemId;

    @Column(nullable = false)
    private int quantity;

    protected TradeLine() {}

    public TradeLine(UUID tradeId, UUID itemId, int quantity) {
        this.tradeId = tradeId;
        this.itemId = itemId;
        this.quantity = quantity;
    }

    public UUID getId()      { return id; }
    public UUID getTradeId() { return tradeId; }
    public UUID getItemId()  { return itemId; }
    public int getQuantity() { return quantity; }
}
