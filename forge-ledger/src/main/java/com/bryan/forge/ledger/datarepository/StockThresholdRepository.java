package com.bryan.forge.ledger.datarepository;

import com.bryan.forge.ledger.datamodel.StockThreshold;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockThresholdRepository extends JpaRepository<StockThreshold, UUID> {

    List<StockThreshold> findByBusinessId(UUID businessId);

    Optional<StockThreshold> findByBusinessIdAndItemId(UUID businessId, UUID itemId);

    void deleteByBusinessIdAndItemId(UUID businessId, UUID itemId);
}
