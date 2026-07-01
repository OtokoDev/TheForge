package com.bryan.forge.business.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/** Liste des clés d'écran à masquer côté front (ex. ["/commandes", "/carte"]). */
@Serdeable
public record SetHiddenScreensRequest(@Nullable List<String> screens) {}
