package com.bryan.forge.billing.datarepository;

import com.bryan.forge.billing.datamodel.PurchaseLine;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PurchaseLineRepository extends JpaRepository<PurchaseLine, UUID> {

    List<PurchaseLine> findByPurchaseId(UUID purchaseId);
}
