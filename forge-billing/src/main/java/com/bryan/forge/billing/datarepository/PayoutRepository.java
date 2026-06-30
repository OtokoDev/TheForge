package com.bryan.forge.billing.datarepository;

import com.bryan.forge.billing.datamodel.Payout;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, UUID> {

    List<Payout> findByBusinessIdOrderByCreatedAtDesc(UUID businessId);
}
