package com.bryan.forge.ledger.datarepository;

import com.bryan.forge.ledger.datamodel.StockBalance;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockBalanceRepository extends JpaRepository<StockBalance, UUID> {

    List<StockBalance> findByAccountId(UUID accountId);

    Optional<StockBalance> findByAccountIdAndItemId(UUID accountId, UUID itemId);
}
