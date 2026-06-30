package com.bryan.forge.billing.datamodel;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Achat de matières auprès d'un fournisseur : septimes sortis du coffre, items entrés au stock. */
@Entity
@Table(name = "purchase")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "business_id", columnDefinition = "uuid", nullable = false)
    private UUID businessId;

    @Column(nullable = false)
    private long numero;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(nullable = false)
    private long total = 0;

    @Column(name = "created_by", columnDefinition = "uuid", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Purchase() {}

    public Purchase(UUID businessId, long numero, String supplierName, long total, UUID createdBy) {
        this.businessId = businessId;
        this.numero = numero;
        this.supplierName = supplierName;
        this.total = total;
        this.createdBy = createdBy;
    }

    public UUID getId()           { return id; }
    public UUID getBusinessId()   { return businessId; }
    public long getNumero()       { return numero; }
    public String getSupplierName() { return supplierName; }
    public long getTotal()        { return total; }
    public UUID getCreatedBy()    { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
