package com.bryan.forge.business.datamodel;

import jakarta.persistence.*;

import java.util.UUID;

/** Type de marqueur configurable (par compagnie) : libellé, couleur, image optionnelle (data URL). */
@Entity
@Table(name = "map_marker_type")
public class MapMarkerType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "business_id", columnDefinition = "uuid", nullable = false)
    private UUID businessId;

    @Column(nullable = false, length = 60)
    private String label;

    @Column(nullable = false, length = 16)
    private String color;

    @Column(name = "image_data_url", columnDefinition = "text")
    private String imageDataUrl;

    protected MapMarkerType() {}

    public MapMarkerType(UUID businessId, String label, String color, String imageDataUrl) {
        this.businessId = businessId;
        this.label = label;
        this.color = color;
        this.imageDataUrl = imageDataUrl;
    }

    public UUID getId()           { return id; }
    public UUID getBusinessId()   { return businessId; }
    public String getLabel()      { return label; }
    public String getColor()      { return color; }
    public String getImageDataUrl() { return imageDataUrl; }
}
