package com.bryan.forge.business.datamodel;

/** Rôle d'un utilisateur au sein d'un business donné (cf. CDC §5.2). */
public enum MembershipRole {
    /** Configure son business (coffres, valeurs, taux, membres) + lecture/écriture. */
    ADMIN,
    /** Opérations de base : factures, sessions, mouvements, consultation. */
    MEMBRE
}
