package com.bryan.forge.billing.datamodel;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Facture d'un business. BROUILLON → VALIDEE. À la validation, les montants sont figés
 * et les mouvements (marchandise + septimes) sont générés (cf. CDC §6.3).
 * Montant effectif payé = total_amount (INTEGER) ; le reste sont des figures comptables.
 */
@Entity
@Table(name = "facture")
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "business_id", columnDefinition = "uuid", nullable = false)
    private UUID businessId;

    @Column(nullable = false)
    private long numero;

    @Column(name = "session_id", columnDefinition = "uuid")
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FactureStatus status = FactureStatus.BROUILLON;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount = 0;

    /** Acompte déjà encaissé via la commande d'origine ; déduit du montant à encaisser à la validation. */
    @Column(nullable = false)
    private long deposit = 0;

    @Column(name = "total_cost", nullable = false, precision = 14, scale = 4)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "total_profit", nullable = false, precision = 14, scale = 4)
    private BigDecimal totalProfit = BigDecimal.ZERO;

    @Column(name = "tax_rate_snapshot", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRateSnapshot = BigDecimal.ZERO;

    @Column(name = "business_share", nullable = false, precision = 14, scale = 4)
    private BigDecimal businessShare = BigDecimal.ZERO;

    @Column(name = "worker_share", nullable = false, precision = 14, scale = 4)
    private BigDecimal workerShare = BigDecimal.ZERO;

    @Column(name = "client_name", length = 100)
    private String clientName;

    /** Facture payée (septims encaissés) ou à crédit (marchandise remise, non encaissée). */
    @Column(nullable = false)
    private boolean paid = false;

    @Column(name = "client_note", length = 1000)
    private String clientNote;

    @Column(name = "internal_note", length = 1000)
    private String internalNote;

    @Column(name = "created_by", columnDefinition = "uuid", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "validated_at")
    private Instant validatedAt;

    protected Facture() {}

    public Facture(UUID businessId, long numero, UUID createdBy, String clientName, String clientNote) {
        this.businessId = businessId;
        this.numero = numero;
        this.createdBy = createdBy;
        this.clientName = clientName;
        this.clientNote = clientNote;
    }

    public UUID getId()               { return id; }
    public UUID getBusinessId()       { return businessId; }
    public long getNumero()           { return numero; }
    public UUID getSessionId()        { return sessionId; }
    public FactureStatus getStatus()  { return status; }
    public long getTotalAmount()      { return totalAmount; }
    public long getDeposit()          { return deposit; }
    public BigDecimal getTotalCost()  { return totalCost; }
    public BigDecimal getTotalProfit(){ return totalProfit; }
    public BigDecimal getTaxRateSnapshot() { return taxRateSnapshot; }
    public BigDecimal getBusinessShare()   { return businessShare; }
    public BigDecimal getWorkerShare()     { return workerShare; }
    public String getClientName()     { return clientName; }
    public boolean isPaid()           { return paid; }
    public String getClientNote()     { return clientNote; }
    public String getInternalNote()   { return internalNote; }
    public UUID getCreatedBy()        { return createdBy; }
    public Instant getCreatedAt()     { return createdAt; }
    public Instant getValidatedAt()   { return validatedAt; }

    public void setSessionId(UUID sessionId)          { this.sessionId = sessionId; }
    public void setStatus(FactureStatus status)       { this.status = status; }
    public void setTotalAmount(long totalAmount)      { this.totalAmount = totalAmount; }
    public void setDeposit(long deposit)              { this.deposit = deposit; }
    public void setTotalCost(BigDecimal totalCost)    { this.totalCost = totalCost; }
    public void setTotalProfit(BigDecimal totalProfit){ this.totalProfit = totalProfit; }
    public void setTaxRateSnapshot(BigDecimal r)      { this.taxRateSnapshot = r; }
    public void setBusinessShare(BigDecimal s)        { this.businessShare = s; }
    public void setWorkerShare(BigDecimal s)          { this.workerShare = s; }
    public void setInternalNote(String internalNote)  { this.internalNote = internalNote; }
    public void setPaid(boolean paid)                 { this.paid = paid; }
    public void setValidatedAt(Instant validatedAt)   { this.validatedAt = validatedAt; }
    public void setClientName(String clientName)      { this.clientName = clientName; }
    public void setClientNote(String clientNote)      { this.clientNote = clientNote; }
}
