package com.bryan.forge.business.datamodel;

import com.bryan.forge.core.datamodel.VersionedEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Un business (tenant) : une forge, une compagnie, etc. Les données sont cloisonnées par business. */
@Entity
@Table(name = "business")
public class Business extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BusinessType type;

    /** Logo en data-URL base64 (ex. "data:image/png;base64,..."), optionnel. */
    @Column(name = "logo_data_url", columnDefinition = "text")
    private String logoDataUrl;

    /** Comptes par défaut utilisés par la caisse (POS) pour émettre une facture. */
    @Column(name = "default_stock_account_id", columnDefinition = "uuid")
    private UUID defaultStockAccountId;

    @Column(name = "default_coffre_account_id", columnDefinition = "uuid")
    private UUID defaultCoffreAccountId;

    /** Écrans masqués côté front (CSV de clés de route), pilotés par SYSTEM. */
    @Column(name = "hidden_screens", nullable = false, columnDefinition = "text")
    private String hiddenScreens = "";

    /** Webhook Discord du business (canal de la faction) ; null → fallback URLs globales. */
    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Business() {}

    public Business(String nom, BusinessType type) {
        this.nom = nom;
        this.type = type;
    }

    public UUID getId()             { return id; }
    public String getNom()          { return nom; }
    public BusinessType getType()   { return type; }
    public String getLogoDataUrl()  { return logoDataUrl; }
    public UUID getDefaultStockAccountId()  { return defaultStockAccountId; }
    public UUID getDefaultCoffreAccountId() { return defaultCoffreAccountId; }
    public String getHiddenScreens() { return hiddenScreens; }
    public String getWebhookUrl()   { return webhookUrl; }
    public Instant getCreatedAt()   { return createdAt; }

    public void setNom(String nom)               { this.nom = nom; }
    public void setType(BusinessType type)       { this.type = type; }
    public void setLogoDataUrl(String logoDataUrl) { this.logoDataUrl = logoDataUrl; }
    public void setDefaultStockAccountId(UUID id)  { this.defaultStockAccountId = id; }
    public void setDefaultCoffreAccountId(UUID id) { this.defaultCoffreAccountId = id; }
    public void setHiddenScreens(String hiddenScreens) { this.hiddenScreens = hiddenScreens == null ? "" : hiddenScreens; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
}
