package com.bryan.forge.billing.datamodel;

/** Cycle de vie d'une commande client (devis → … → livrée). */
public enum CommandeStatus {
    DEVIS,
    CONFIRMEE,
    EN_PRODUCTION,
    PRETE,
    LIVREE,
    ANNULEE
}
