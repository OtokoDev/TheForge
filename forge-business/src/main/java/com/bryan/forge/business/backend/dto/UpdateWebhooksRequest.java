package com.bryan.forge.business.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record UpdateWebhooksRequest(boolean enabled) {}
