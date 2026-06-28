package com.bryan.forge.business.backend.dto;

import com.bryan.forge.business.datamodel.Membership;
import com.bryan.forge.business.datamodel.MembershipRole;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

/** Vue « côté business » : qui sont les membres d'un business. */
@Serdeable
public record MemberDto(UUID userId, String username, String inGameName, MembershipRole role, int version) {
    public static MemberDto from(Membership m) {
        return new MemberDto(m.getUser().getId(), m.getUser().getUsername(), m.getUser().getInGameName(),
                m.getRole(), m.getVersion());
    }
}
