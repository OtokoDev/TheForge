package com.bryan.forge.treasury.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

@Serdeable
public record DepositLine(UUID itemId, int quantity) {}
