package com.bryan.forge.catalog.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;
import java.util.UUID;

/** Nouvel ordre : ids dans l'ordre voulu (ordre = index). */
@Serdeable
public record ReorderRequest(List<UUID> ids) {}
