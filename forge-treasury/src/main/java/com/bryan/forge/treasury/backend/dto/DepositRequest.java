package com.bryan.forge.treasury.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;
import java.util.UUID;

/** Dépôt d'un farmeur : items déposés dans un compte stock, valorisés en créance. */
@Serdeable
public record DepositRequest(UUID farmerUserId, List<DepositLine> lines, UUID stockAccountId, @Nullable String reference) {}
