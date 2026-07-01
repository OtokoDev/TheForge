package com.bryan.forge.billing.datarepository;

import com.bryan.forge.billing.datamodel.TradeLine;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeLineRepository extends JpaRepository<TradeLine, UUID> {

    List<TradeLine> findByTradeId(UUID tradeId);
}
