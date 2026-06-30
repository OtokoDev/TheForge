package com.bryan.forge.business.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record CreateMarkerTypeRequest(String label, String color, @Nullable String imageDataUrl) {}
