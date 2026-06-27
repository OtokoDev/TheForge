package com.bryan.forge.business.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;

/** Entrée de main courante prête pour le front (pseudo de l'auteur résolu). */
@Serdeable
public record ActivityDto(String action, @Nullable String details, String username, Instant createdAt) {}
