package com.bryan.forge.treasury.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

/** Solde de créance d'un farmeur (nom libre) : reste dû = totalCredit − totalPaid. */
@Serdeable
public record CreanceFarmerDto(String farmerName, long totalCredit, long totalPaid, long remaining) {}
