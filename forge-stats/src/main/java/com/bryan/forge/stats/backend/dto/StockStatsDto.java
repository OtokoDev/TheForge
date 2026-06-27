package com.bryan.forge.stats.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/** E — Stock : valeur (au coût), ruptures/stock faible, top matières consommées. */
@Serdeable
public record StockStatsDto(
        long valeurStock,
        List<NameValue> ruptures,
        List<NameValue> topConsommees) {}
