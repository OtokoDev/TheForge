package com.bryan.forge.billing.datarepository;

import com.bryan.forge.billing.datamodel.Trade;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeRepository extends JpaRepository<Trade, UUID> {

    /** Échanges où le business est vendeur ou acheteur, plus récents en premier. */
    List<Trade> findByFromBusinessIdOrToBusinessIdOrderByCreatedAtDesc(UUID fromBusinessId, UUID toBusinessId);

    @Query(value = "SELECT nextval('trade_numero_seq')", nativeQuery = true)
    long nextNumero();
}
