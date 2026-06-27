package com.bryan.forge.stats.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/** B — Produits : top (CA/marge/qté), ventilation famille/matériau, alertes vendu à perte. */
@Serdeable
public record ProductsDto(
        List<ProductStat> top,
        List<NameValue> parFamille,
        List<NameValue> parMateriau,
        List<LossAlert> pertes) {

    @Serdeable
    public record ProductStat(String itemId, String name, long ca, long marge, long qte) {}

    @Serdeable
    public record LossAlert(String name, long prixRevente, long cout) {}
}
