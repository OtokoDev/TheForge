package com.bryan.forge.billing.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

/**
 * Compte de résultat synthétique (factures validées).
 * resultat = partBusiness − depenses (ce que l'entreprise garde, net de ses charges).
 */
@Serdeable
public record FinanceSummaryDto(
        long caEncaisse,
        long coutTotal,
        long benefice,
        long partBusiness,
        long partForgerons,
        long paieVersee,
        long depenses,
        long resultat
) {}
