package com.bryan.forge.business.backend.dto;

import com.bryan.forge.business.datamodel.BusinessType;
import com.bryan.forge.business.datamodel.Membership;
import com.bryan.forge.business.datamodel.MembershipRole;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

/** Vue « côté utilisateur » : à quels business j'appartiens et avec quel rôle. */
@Serdeable
public record MembershipDto(UUID businessId, String businessNom, BusinessType businessType, MembershipRole role) {
    public static MembershipDto from(Membership m) {
        return new MembershipDto(m.getBusiness().getId(), m.getBusiness().getNom(),
                m.getBusiness().getType(), m.getRole());
    }
}
