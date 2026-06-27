package com.bryan.forge.catalog.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record UpdateTaxonRequest(String nom, int ordre, @Nullable String couleur, int version) {}
