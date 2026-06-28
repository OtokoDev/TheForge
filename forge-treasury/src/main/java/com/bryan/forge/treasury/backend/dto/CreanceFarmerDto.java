package com.bryan.forge.treasury.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

/** Solde de créance d'un farmeur : reste dû = totalCredit − totalPaid. */
@Serdeable
public record CreanceFarmerDto(UUID farmerUserId, String farmerUsername, String farmerInGameName, long totalCredit, long totalPaid, long remaining) {}
