package com.bryan.forge.core.datamodel;

/**
 * Rôle global d'un utilisateur (cf. CDC §5.2). Fixe, non configurable.
 * Les rôles par business (ADMIN / MEMBRE) sont portés par {@code Membership}, pas ici.
 */
public enum GlobalRole {
    /** « Big boss » : tout, partout (catalogue global, création de business, gestion des users). */
    SYSTEM,
    /** Lecture seule sur le stock et les factures de tous les business. */
    STAFF,
    /** Aucun privilège global : n'a accès qu'à ses business via ses memberships. */
    NONE
}
