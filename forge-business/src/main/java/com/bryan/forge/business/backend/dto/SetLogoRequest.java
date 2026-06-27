package com.bryan.forge.business.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record SetLogoRequest(@Nullable String dataUrl) {}
