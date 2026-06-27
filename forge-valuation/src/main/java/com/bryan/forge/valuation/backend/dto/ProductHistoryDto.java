package com.bryan.forge.valuation.backend.dto;

import com.bryan.forge.valuation.datamodel.Product;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.time.Instant;

@Serdeable
public record ProductHistoryDto(@Nullable BigDecimal valeur, @Nullable BigDecimal prixRevente,
                                Instant validFrom, @Nullable Instant validTo) {
    public static ProductHistoryDto from(Product p) {
        return new ProductHistoryDto(p.getValeur(), p.getPrixRevente(), p.getValidFrom(), p.getValidTo());
    }
}
