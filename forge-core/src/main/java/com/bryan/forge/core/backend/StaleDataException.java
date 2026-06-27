package com.bryan.forge.core.backend;

/** Conflit de version optimiste : l'entité a été modifiée entre-temps (→ HTTP 409). */
public class StaleDataException extends RuntimeException {
    public StaleDataException(String message) {
        super(message);
    }

    /** Vérifie que la version attendue (front) correspond à la version courante (base). */
    public static void check(int currentVersion, int expectedVersion) {
        if (currentVersion != expectedVersion) {
            throw new StaleDataException("Données modifiées entre-temps — actualisez puis réessayez");
        }
    }
}
