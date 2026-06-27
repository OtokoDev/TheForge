package com.bryan.forge.valuation.datamodel;

import com.bryan.forge.core.datamodel.VersionedEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Produit = un item rattaché à un business, à une période donnée. Porte deux chiffres
 * distincts (CDC §6.2) :
 * <ul>
 *   <li>{@code valeur} : coût/valeur unitaire (base du coût de revient). Saisi pour les
 *       matières (feuilles) ; null pour un craftable (dérivé de la recette).</li>
 *   <li>{@code prixRevente} : prix de vente client (indépendant, null = non vendable).</li>
 * </ul>
 * La version courante a {@code validTo == null} ; chaque changement clôture la précédente
 * et en crée une nouvelle (append-only → historique dans le temps).
 */
@Entity
@Table(name = "product")
public class Product extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "business_id", columnDefinition = "uuid", nullable = false)
    private UUID businessId;

    @Column(name = "item_id", columnDefinition = "uuid", nullable = false)
    private UUID itemId;

    @Column(name = "valeur", precision = 12, scale = 4)
    private BigDecimal valeur;

    @Column(name = "prix_revente", precision = 12, scale = 4)
    private BigDecimal prixRevente;

    @Column(name = "valid_from", nullable = false, updatable = false)
    private Instant validFrom = Instant.now();

    @Column(name = "valid_to")
    private Instant validTo;

    protected Product() {}

    public Product(UUID businessId, UUID itemId, BigDecimal valeur, BigDecimal prixRevente) {
        this.businessId = businessId;
        this.itemId = itemId;
        this.valeur = valeur;
        this.prixRevente = prixRevente;
    }

    public UUID getId()             { return id; }
    public UUID getBusinessId()     { return businessId; }
    public UUID getItemId()         { return itemId; }
    public BigDecimal getValeur()       { return valeur; }
    public BigDecimal getPrixRevente()  { return prixRevente; }
    public Instant getValidFrom()   { return validFrom; }
    public Instant getValidTo()     { return validTo; }

    public void setValidTo(Instant validTo) { this.validTo = validTo; }
}
