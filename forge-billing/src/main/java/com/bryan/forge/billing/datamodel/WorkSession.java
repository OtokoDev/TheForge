package com.bryan.forge.billing.datamodel;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Prise de poste d'un travailleur dans un business (cf. CDC §6.5). Les factures créées
 * pendant le poste y sont rattachées ; à la fermeture, un récap est figé.
 */
@Entity
@Table(name = "work_session")
public class WorkSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "business_id", columnDefinition = "uuid", nullable = false)
    private UUID businessId;

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "tax_rate_snapshot", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRateSnapshot = BigDecimal.ZERO;

    // ── Récap figé à la fermeture ──────────────────────────────────────────────
    @Column(name = "orders_count", nullable = false)
    private int ordersCount = 0;

    @Column(name = "total_sales", nullable = false)
    private long totalSales = 0;

    @Column(name = "total_cost", nullable = false, precision = 14, scale = 4)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "total_profit", nullable = false, precision = 14, scale = 4)
    private BigDecimal totalProfit = BigDecimal.ZERO;

    @Column(name = "business_share", nullable = false, precision = 14, scale = 4)
    private BigDecimal businessShare = BigDecimal.ZERO;

    @Column(name = "worker_share", nullable = false, precision = 14, scale = 4)
    private BigDecimal workerShare = BigDecimal.ZERO;

    protected WorkSession() {}

    public WorkSession(UUID businessId, UUID userId, BigDecimal taxRateSnapshot) {
        this.businessId = businessId;
        this.userId = userId;
        this.taxRateSnapshot = taxRateSnapshot;
    }

    public UUID getId()            { return id; }
    public UUID getBusinessId()    { return businessId; }
    public UUID getUserId()        { return userId; }
    public Instant getOpenedAt()   { return openedAt; }
    public Instant getClosedAt()   { return closedAt; }
    public BigDecimal getTaxRateSnapshot() { return taxRateSnapshot; }
    public int getOrdersCount()    { return ordersCount; }
    public long getTotalSales()    { return totalSales; }
    public BigDecimal getTotalCost()   { return totalCost; }
    public BigDecimal getTotalProfit() { return totalProfit; }
    public BigDecimal getBusinessShare() { return businessShare; }
    public BigDecimal getWorkerShare()   { return workerShare; }

    public void close(int ordersCount, long totalSales, BigDecimal totalCost, BigDecimal totalProfit,
                      BigDecimal businessShare, BigDecimal workerShare) {
        this.closedAt = Instant.now();
        this.ordersCount = ordersCount;
        this.totalSales = totalSales;
        this.totalCost = totalCost;
        this.totalProfit = totalProfit;
        this.businessShare = businessShare;
        this.workerShare = workerShare;
    }
}
