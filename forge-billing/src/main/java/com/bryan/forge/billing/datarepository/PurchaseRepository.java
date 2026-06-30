package com.bryan.forge.billing.datarepository;

import com.bryan.forge.billing.datamodel.Purchase;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {

    List<Purchase> findByBusinessIdOrderByNumeroDesc(UUID businessId);

    @Query(value = "SELECT nextval('purchase_numero_seq')", nativeQuery = true)
    long nextNumero();
}
