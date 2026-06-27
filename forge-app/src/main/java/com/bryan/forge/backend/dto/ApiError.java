package com.bryan.forge.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

/** Corps d'erreur simple et lisible pour le front : { "message": "..." }. */
@Serdeable
public record ApiError(String message) {}
