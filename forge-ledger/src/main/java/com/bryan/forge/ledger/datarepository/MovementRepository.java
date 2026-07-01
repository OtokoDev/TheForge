package com.bryan.forge.ledger.datarepository;

import com.bryan.forge.ledger.datamodel.Movement;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MovementRepository extends JpaRepository<Movement, UUID> {

    /** Tous les mouvements touchant un compte (entrée ou sortie) → base des projections. */
    List<Movement> findByFromAccountIdOrToAccountId(UUID fromAccountId, UUID toAccountId);

    /** Journal du business, plus récent en premier. */
    List<Movement> findByBusinessIdOrderByCreatedAtDesc(UUID businessId);

    /** Mouvements liés à une référence métier (ex. FACTURE) — base des avoirs. */
    List<Movement> findByReferenceTypeAndReferenceIdOrderByCreatedAtAsc(String referenceType, UUID referenceId);
}
