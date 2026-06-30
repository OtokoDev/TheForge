package com.bryan.forge.billing.backend.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/** Lignes : {@link CreateFactureLine} où {@code unitPrice} = coût d'achat unitaire. */
@Serdeable
public record CreatePurchaseRequest(@Nullable String supplierName, List<CreateFactureLine> lines) {}
