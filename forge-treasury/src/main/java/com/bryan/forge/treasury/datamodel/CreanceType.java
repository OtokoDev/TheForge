package com.bryan.forge.treasury.datamodel;

/** Type d'écriture de créance (cf. CDC §4.2). */
public enum CreanceType {
    /** Dette créée par un dépôt valorisé du farmeur. */
    CREDIT,
    /** Remboursement (paiement) au farmeur. */
    PAIEMENT
}
