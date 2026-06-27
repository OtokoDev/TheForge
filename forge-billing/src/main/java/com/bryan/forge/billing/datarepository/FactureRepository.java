package com.bryan.forge.billing.datarepository;

import com.bryan.forge.billing.datamodel.Facture;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FactureRepository extends JpaRepository<Facture, UUID> {

    List<Facture> findByBusinessIdOrderByNumeroDesc(UUID businessId);

    List<Facture> findBySessionId(UUID sessionId);

    /** Prochain numéro de facture (séquence globale). */
    @Query(value = "SELECT nextval('facture_numero_seq')", nativeQuery = true)
    long nextNumero();
}
