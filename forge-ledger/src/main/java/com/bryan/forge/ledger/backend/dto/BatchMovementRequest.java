package com.bryan.forge.ledger.backend.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/** Lot de mouvements à enregistrer atomiquement (ex. dépôt de plusieurs objets). */
@Serdeable
public record BatchMovementRequest(List<RecordMovementRequest> moves) {
}
