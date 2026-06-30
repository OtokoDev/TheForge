package com.bryan.forge.billing.datarepository;

import com.bryan.forge.billing.datamodel.Commande;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, UUID> {

    List<Commande> findByBusinessIdOrderByNumeroDesc(UUID businessId);

    /** Prochain numéro de commande (séquence globale). */
    @Query(value = "SELECT nextval('commande_numero_seq')", nativeQuery = true)
    long nextNumero();
}
