package com.bryan.forge.business.backend.dto;

import com.bryan.forge.business.datamodel.MembershipRole;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

@Serdeable
/** {@code version} : version attendue d'un membre existant (verrou optimiste sur le rôle). */
public record AddMemberRequest(UUID userId, MembershipRole role, int version) {}
