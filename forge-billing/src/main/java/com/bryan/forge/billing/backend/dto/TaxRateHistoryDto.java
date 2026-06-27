package com.bryan.forge.billing.backend.dto;

import com.bryan.forge.billing.datamodel.TaxRate;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.time.Instant;

@Serdeable
public record TaxRateHistoryDto(BigDecimal rate, Instant validFrom, @Nullable Instant validTo) {
    public static TaxRateHistoryDto from(TaxRate t) {
        return new TaxRateHistoryDto(t.getRate(), t.getValidFrom(), t.getValidTo());
    }
}
