package com.bryan.forge.stats.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

/** Couple nom → valeur (ventilations, classements). */
@Serdeable
public record NameValue(String nom, long valeur) {}
