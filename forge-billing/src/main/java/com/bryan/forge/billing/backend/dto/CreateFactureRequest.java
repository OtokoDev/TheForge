package com.bryan.forge.billing.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

@Serdeable
public record CreateFactureRequest(List<CreateFactureLine> lines, @Nullable String clientName, @Nullable String clientNote) {}
