package com.bryan.forge.billing.datamodel;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Commande client d'un business. DEVIS → CONFIRMEE → EN_PRODUCTION → PRETE → LIVREE
 * (ou ANNULEE). À la livraison, convertie en facture BROUILLON (factureId renseigné).
 */
@Entity
@Table(name = "commande")
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "business_id", columnDefinition = "uuid", nullable = false)
    private UUID businessId;

    @Column(nullable = false)
    private long numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommandeStatus status = CommandeStatus.DEVIS;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "client_note", length = 1000)
    private String clientNote;

    @Column(name = "due_date")
    private Instant dueDate;

    /** Acompte versé (septimes). */
    @Column(nullable = false)
    private long acompte = 0;

    /** Facture issue de la commande (renseignée à la livraison). */
    @Column(name = "facture_id", columnDefinition = "uuid")
    private UUID factureId;

    @Column(name = "created_by", columnDefinition = "uuid", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Commande() {}

    public Commande(UUID businessId, long numero, UUID createdBy, String clientName,
                    String clientNote, Instant dueDate, long acompte) {
        this.businessId = businessId;
        this.numero = numero;
        this.createdBy = createdBy;
        this.clientName = clientName;
        this.clientNote = clientNote;
        this.dueDate = dueDate;
        this.acompte = acompte;
    }

    public UUID getId()             { return id; }
    public UUID getBusinessId()     { return businessId; }
    public long getNumero()         { return numero; }
    public CommandeStatus getStatus() { return status; }
    public String getClientName()   { return clientName; }
    public String getClientNote()   { return clientNote; }
    public Instant getDueDate()     { return dueDate; }
    public long getAcompte()        { return acompte; }
    public UUID getFactureId()      { return factureId; }
    public UUID getCreatedBy()      { return createdBy; }
    public Instant getCreatedAt()   { return createdAt; }

    public void setStatus(CommandeStatus status)   { this.status = status; }
    public void setClientName(String clientName)   { this.clientName = clientName; }
    public void setClientNote(String clientNote)   { this.clientNote = clientNote; }
    public void setDueDate(Instant dueDate)        { this.dueDate = dueDate; }
    public void setAcompte(long acompte)           { this.acompte = acompte; }
    public void setFactureId(UUID factureId)       { this.factureId = factureId; }
}
