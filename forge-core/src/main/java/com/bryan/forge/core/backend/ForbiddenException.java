package com.bryan.forge.core.backend;

/** Levée quand l'utilisateur est authentifié mais n'a pas le droit d'agir (→ HTTP 403). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
