package com.bryan.forge.core.datamodel;

import java.util.UUID;

/** Identifiants d'acteurs non-humains pour la traçabilité (created_by / modified_by). */
public final class SystemActors {

    /** Auteur « système » : action hors requête utilisateur (CRON, tâche async, seed…). */
    public static final UUID SYSTEM_ID = new UUID(0L, 0L);

    private SystemActors() {}

    public static boolean isSystem(UUID id) {
        return SYSTEM_ID.equals(id);
    }
}
