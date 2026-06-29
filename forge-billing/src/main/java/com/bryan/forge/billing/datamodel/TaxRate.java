package com.bryan.forge.billing.datamodel;

import com.bryan.forge.core.datamodel.VersionedEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Taux de taxe d'un business à une période. Découpe le bénéfice d'une facture entre
 * part business et part travailleur. Historisé (append-only) ; version courante = validTo null.
 */
@Entity
@Table(name = "tax_rate")
public class TaxRate extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "business_id", columnDefinition = "uuid", nullable = false)
    private UUID businessId;

    /** Fraction prélevée pour la part business (0..1). */
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal rate;

    /** Assiette : sur le bénéfice (PROFIT, défaut) ou sur le chiffre d'affaires (REVENUE). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TaxBase base = TaxBase.PROFIT;

    @Column(name = "valid_from", nullable = false, updatable = false)
    private Instant validFrom = Instant.now();

    @Column(name = "valid_to")
    private Instant validTo;

    protected TaxRate() {}

    public TaxRate(UUID businessId, BigDecimal rate, TaxBase base) {
        this.businessId = businessId;
        this.rate = rate;
        this.base = base;
    }

    public UUID getId()           { return id; }
    public UUID getBusinessId()   { return businessId; }
    public BigDecimal getRate()   { return rate; }
    public TaxBase getBase()      { return base; }
    public Instant getValidFrom() { return validFrom; }
    public Instant getValidTo()   { return validTo; }

    public void setValidTo(Instant validTo) { this.validTo = validTo; }
}
