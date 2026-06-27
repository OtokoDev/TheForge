package com.bryan.forge.business.backend.dto;

import com.bryan.forge.core.backend.dto.UserDto;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/** Réponse de {@code GET /api/me} : profil de l'utilisateur connecté + ses appartenances. */
@Serdeable
public record MeDto(UserDto user, List<MembershipDto> memberships) {}
