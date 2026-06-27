package com.bryan.forge.billing.datamodel;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ligne de facture. {@code unitPriceSnapshot} est pré-rempli depuis la valuation et
 * modifiable tant que la facture est BROUILLON ; figé à la validation, avec le coût.
 */
@Entity
@Table(name = "facture_line")
public class FactureLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "facture_id", columnDefinition = "uuid", nullable = false)
    private UUID factureId;

    @Column(name = "item_id", columnDefinition = "uuid", nullable = false)
    private UUID itemId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitPriceSnapshot = BigDecimal.ZERO;

    @Column(name = "unit_cost_snapshot", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitCostSnapshot = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false)
    private long lineTotal = 0;

    protected FactureLine() {}

    public FactureLine(UUID factureId, UUID itemId, int quantity, BigDecimal unitPriceSnapshot) {
        this.factureId = factureId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.unitPriceSnapshot = unitPriceSnapshot;
    }

    public UUID getId()                   { return id; }
    public UUID getFactureId()            { return factureId; }
    public UUID getItemId()               { return itemId; }
    public int getQuantity()              { return quantity; }
    public BigDecimal getUnitPriceSnapshot() { return unitPriceSnapshot; }
    public BigDecimal getUnitCostSnapshot()  { return unitCostSnapshot; }
    public long getLineTotal()            { return lineTotal; }

    public void setQuantity(int quantity)                  { this.quantity = quantity; }
    public void setUnitPriceSnapshot(BigDecimal price)     { this.unitPriceSnapshot = price; }
    public void setUnitCostSnapshot(BigDecimal cost)       { this.unitCostSnapshot = cost; }
    public void setLineTotal(long lineTotal)               { this.lineTotal = lineTotal; }
}
