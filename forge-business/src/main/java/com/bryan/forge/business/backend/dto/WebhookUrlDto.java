package com.bryan.forge.business.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

/** Webhook Discord du business (null = fallback URLs globales). */
@Serdeable
public record WebhookUrlDto(@Nullable String url) {}
