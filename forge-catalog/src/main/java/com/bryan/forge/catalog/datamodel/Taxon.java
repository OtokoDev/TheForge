package com.bryan.forge.catalog.datamodel;

import com.bryan.forge.core.datamodel.VersionedEntity;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * Élément d'une liste de classement configurable par SYSTEM. Deux dimensions distinctes
 * ({@link TaxonKind}) : famille et matériau. {@code ordre} sert au tri d'affichage.
 */
@Entity
@Table(name = "taxon", uniqueConstraints = @UniqueConstraint(columnNames = {"kind", "nom"}))
@Serdeable
public class Taxon extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaxonKind kind;

    @Column(nullable = false, length = 60)
    private String nom;

    @Column(nullable = false)
    private int ordre;

    /** Couleur hex (ex. "#E8590C") pour les puces UI, optionnelle. */
    @Column(length = 9)
    private String couleur;

    protected Taxon() {}

    public Taxon(TaxonKind kind, String nom, int ordre, String couleur) {
        this.kind = kind;
        this.nom = nom;
        this.ordre = ordre;
        this.couleur = couleur;
    }

    public UUID getId()        { return id; }
    public TaxonKind getKind() { return kind; }
    public String getNom()     { return nom; }
    public int getOrdre()      { return ordre; }
    public String getCouleur() { return couleur; }

    public void setNom(String nom)         { this.nom = nom; }
    public void setOrdre(int ordre)        { this.ordre = ordre; }
    public void setCouleur(String couleur) { this.couleur = couleur; }
}
