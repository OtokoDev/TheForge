package com.bryan.forge.billing.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

/** État du poste de l'utilisateur courant dans le business (pour l'indicateur global). */
@Serdeable
public record ShiftStatusDto(boolean open, @Nullable SessionDto session) {}
