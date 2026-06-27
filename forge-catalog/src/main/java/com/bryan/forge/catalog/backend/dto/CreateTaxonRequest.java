package com.bryan.forge.catalog.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record CreateTaxonRequest(String nom, @Nullable String couleur) {}
