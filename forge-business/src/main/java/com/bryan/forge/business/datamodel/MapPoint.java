package com.bryan.forge.business.datamodel;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Point d'intérêt RP sur la carte de Bordeciel, par business (compagnies). x/y = pixels natifs. */
@Entity
@Table(name = "map_point")
public class MapPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "business_id", columnDefinition = "uuid", nullable = false)
    private UUID businessId;

    @Column(nullable = false, length = 64)
    private String type;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(nullable = false)
    private int x;

    @Column(nullable = false)
    private int y;

    @Column(length = 500)
    private String note;

    @Column(name = "created_by", columnDefinition = "uuid", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected MapPoint() {}

    public MapPoint(UUID businessId, String type, String label, int x, int y, String note, UUID createdBy) {
        this.businessId = businessId;
        this.type = type;
        this.label = label;
        this.x = x;
        this.y = y;
        this.note = note;
        this.createdBy = createdBy;
    }

    public UUID getId()         { return id; }
    public UUID getBusinessId() { return businessId; }
    public String getType()     { return type; }
    public String getLabel()    { return label; }
    public int getX()           { return x; }
    public int getY()           { return y; }
    public String getNote()     { return note; }
    public UUID getCreatedBy()  { return createdBy; }

    public void setType(String type)   { this.type = type; }
    public void setLabel(String label) { this.label = label; }
    public void setNote(String note)   { this.note = note; }
}
