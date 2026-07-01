package com.bryan.forge.treasury.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.util.UUID;

/** {@code unitPrice} = prix d'achat négocié (optionnel) ; si null, valorisé au coût courant. */
@Serdeable
public record DepositLine(UUID itemId, int quantity, @Nullable BigDecimal unitPrice) {}
