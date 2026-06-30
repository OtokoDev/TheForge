package com.bryan.forge.billing.datarepository;

import com.bryan.forge.billing.datamodel.ProductionOrder;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, UUID> {

    List<ProductionOrder> findByBusinessIdOrderByNumeroDesc(UUID businessId);

    /** Prochain numéro d'ordre de fabrication (séquence globale). */
    @Query(value = "SELECT nextval('production_numero_seq')", nativeQuery = true)
    long nextNumero();
}
