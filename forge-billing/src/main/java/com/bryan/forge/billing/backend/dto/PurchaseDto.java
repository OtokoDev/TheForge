package com.bryan.forge.billing.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Serdeable
public record PurchaseDto(UUID id, long numero, @Nullable String supplierName, long total,
                          Instant createdAt, List<PurchaseLineDto> lines) {}
