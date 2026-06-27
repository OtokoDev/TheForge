package com.bryan.forge.billing.datarepository;

import com.bryan.forge.billing.datamodel.TaxRate;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, UUID> {

    Optional<TaxRate> findByBusinessIdAndValidToIsNull(UUID businessId);

    List<TaxRate> findByBusinessIdOrderByValidFromDesc(UUID businessId);
}
