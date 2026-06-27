package com.bryan.forge.billing.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

/**
 * Validation/émission. {@code paid} : encaissé (septims → coffre) ou à crédit (non encaissé).
 * Comptes optionnels : si nuls, on prend les comptes par défaut du business (POS).
 */
@Serdeable
public record ValidateFactureRequest(boolean paid, @Nullable UUID stockAccountId, @Nullable UUID coffreAccountId) {}
