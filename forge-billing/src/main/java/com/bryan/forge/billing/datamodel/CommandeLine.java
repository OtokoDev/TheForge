package com.bryan.forge.billing.datamodel;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "commande_line")
public class CommandeLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "commande_id", columnDefinition = "uuid", nullable = false)
    private UUID commandeId;

    @Column(name = "item_id", columnDefinition = "uuid", nullable = false)
    private UUID itemId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitPriceSnapshot = BigDecimal.ZERO;

    protected CommandeLine() {}

    public CommandeLine(UUID commandeId, UUID itemId, int quantity, BigDecimal unitPriceSnapshot) {
        this.commandeId = commandeId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.unitPriceSnapshot = unitPriceSnapshot;
    }

    public UUID getId()                  { return id; }
    public UUID getCommandeId()          { return commandeId; }
    public UUID getItemId()              { return itemId; }
    public int getQuantity()             { return quantity; }
    public BigDecimal getUnitPriceSnapshot() { return unitPriceSnapshot; }
}
