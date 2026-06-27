package com.bryan.forge.catalog.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

@Serdeable
public record RecipeLine(UUID componentItemId, int quantity) {}
