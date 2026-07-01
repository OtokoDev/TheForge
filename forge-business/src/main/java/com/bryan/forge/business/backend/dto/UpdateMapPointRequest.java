package com.bryan.forge.business.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

/** Édition d'un marqueur (position inchangée). */
@Serdeable
public record UpdateMapPointRequest(String type, String label, @Nullable String note) {}
