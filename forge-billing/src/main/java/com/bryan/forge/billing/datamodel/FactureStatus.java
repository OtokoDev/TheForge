package com.bryan.forge.billing.datamodel;

/** Cycle de vie d'une facture (cf. CDC §6.3). Pas d'annulation en v1. */
public enum FactureStatus {
    BROUILLON,
    VALIDEE
}
