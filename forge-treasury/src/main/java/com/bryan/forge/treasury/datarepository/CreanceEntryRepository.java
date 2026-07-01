package com.bryan.forge.treasury.datarepository;

import com.bryan.forge.treasury.datamodel.CreanceEntry;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CreanceEntryRepository extends JpaRepository<CreanceEntry, UUID> {

    List<CreanceEntry> findByBusinessId(UUID businessId);

    List<CreanceEntry> findByBusinessIdAndFarmerNameOrderByCreatedAtDesc(UUID businessId, String farmerName);
}
