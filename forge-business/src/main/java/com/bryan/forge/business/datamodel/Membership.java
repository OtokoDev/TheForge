package com.bryan.forge.business.datamodel;

import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.core.datamodel.VersionedEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Appartenance d'un utilisateur à un business, avec son rôle local. Unique par (user, business). */
@Entity
@Table(name = "membership", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "business_id"}))
public class Membership extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Membership() {}

    public Membership(User user, Business business, MembershipRole role) {
        this.user = user;
        this.business = business;
        this.role = role;
    }

    public UUID getId()            { return id; }
    public User getUser()          { return user; }
    public Business getBusiness()  { return business; }
    public MembershipRole getRole() { return role; }
    public Instant getCreatedAt()  { return createdAt; }

    public void setRole(MembershipRole role) { this.role = role; }
}
