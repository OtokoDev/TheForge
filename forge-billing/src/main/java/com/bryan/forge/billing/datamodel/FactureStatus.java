package com.bryan.forge.billing.datamodel;

/** Cycle de vie d'une facture (cf. CDC §6.3). ANNULEE = avoir : mouvements inversés. */
public enum FactureStatus {
    BROUILLON,
    VALIDEE,
    ANNULEE
}
