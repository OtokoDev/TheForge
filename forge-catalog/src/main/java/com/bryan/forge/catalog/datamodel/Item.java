package com.bryan.forge.catalog.datamodel;

import com.bryan.forge.core.datamodel.VersionedEntity;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Item global, commun à tous les business (minerai, lingot, épée… et le septime).
 * Les quantités sont toujours entières ; la valeur est portée ailleurs (par business).
 */
@Entity
@Table(name = "item")
@Serdeable
public class Item extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Famille (épée, casque…) et matériau (acier, bois…) : deux dimensions de classement. */
    @Column(name = "family_id", columnDefinition = "uuid")
    private UUID familyId;

    @Column(name = "material_id", columnDefinition = "uuid")
    private UUID materialId;

    @Enumerated(EnumType.STRING)
    @Column(name = "hand_required", length = 10)
    private HandRequired handRequired;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** Item système (le septime) : non supprimable, non désactivable. */
    @Column(name = "is_system", nullable = false)
    private boolean system = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Item() {}

    public Item(String name, UUID familyId, UUID materialId, HandRequired handRequired) {
        this.name = name;
        this.familyId = familyId;
        this.materialId = materialId;
        this.handRequired = handRequired;
    }

    public UUID getId()                  { return id; }
    public String getName()              { return name; }
    public UUID getFamilyId()            { return familyId; }
    public UUID getMaterialId()          { return materialId; }
    public HandRequired getHandRequired() { return handRequired; }
    public boolean isActive()            { return active; }
    public boolean isSystem()            { return system; }
    public Instant getCreatedAt()        { return createdAt; }

    public void setName(String name)                       { this.name = name; }
    public void setFamilyId(UUID familyId)                 { this.familyId = familyId; }
    public void setMaterialId(UUID materialId)             { this.materialId = materialId; }
    public void setHandRequired(HandRequired handRequired) { this.handRequired = handRequired; }
    public void setActive(boolean active)                  { this.active = active; }
}
