package com.bryan.forge.treasury.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

/** Paiement d'un farmeur (nom libre) : septimes sortis d'un coffre. */
@Serdeable
public record PaymentRequest(String farmerName, long amount, UUID coffreAccountId, @Nullable String reference) {}
