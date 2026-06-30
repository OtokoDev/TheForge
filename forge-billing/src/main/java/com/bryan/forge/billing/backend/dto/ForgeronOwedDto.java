package com.bryan.forge.billing.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

/** Part d'un forgeron : gagnée (Σ parts des factures validées), versée, et reste dû. */
@Serdeable
public record ForgeronOwedDto(UUID userId, String name, long earned, long paid, long owed) {}
