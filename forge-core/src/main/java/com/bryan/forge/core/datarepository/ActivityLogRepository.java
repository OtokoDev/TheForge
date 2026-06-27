package com.bryan.forge.core.datarepository;

import com.bryan.forge.core.datamodel.ActivityLog;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import io.micronaut.data.model.Pageable;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    List<ActivityLog> findByBusinessIdOrderByCreatedAtDesc(UUID businessId);

    List<ActivityLog> findByBusinessIdOrderByCreatedAtDesc(UUID businessId, Pageable pageable);

    /** Événements globaux (sans business) = main courante SYSTEM. */
    List<ActivityLog> findByBusinessIdIsNullOrderByCreatedAtDesc(Pageable pageable);
}
