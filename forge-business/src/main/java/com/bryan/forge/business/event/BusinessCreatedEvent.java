package com.bryan.forge.business.event;

import java.util.UUID;

/** Émis après création d'un business — déclenche l'init du compte stock/coffre par défaut (forge-ledger). */
public record BusinessCreatedEvent(UUID businessId) {
}
