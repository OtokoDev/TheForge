package com.bryan.forge.ledger.datamodel;

/** Nature d'un mouvement (cf. CDC §4.2). Sert au classement / à l'audit du journal. */
public enum MovementType {
    PRODUCTION,
    CONSUMPTION,
    SALE,
    PURCHASE,
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER,
    /** Régularisation suite à un inventaire (écart site / jeu). */
    ADJUSTMENT
}
