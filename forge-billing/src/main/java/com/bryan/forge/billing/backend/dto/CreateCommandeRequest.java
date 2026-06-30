package com.bryan.forge.billing.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.List;

/** Création / mise à jour d'une commande. Lignes réutilisent {@link CreateFactureLine}. */
@Serdeable
public record CreateCommandeRequest(
        @Nullable String clientName,
        @Nullable String clientNote,
        @Nullable Instant dueDate,
        @Nullable Long acompte,
        List<CreateFactureLine> lines
) {}
