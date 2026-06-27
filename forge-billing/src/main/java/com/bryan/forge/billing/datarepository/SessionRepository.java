package com.bryan.forge.billing.datarepository;

import com.bryan.forge.billing.datamodel.WorkSession;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<WorkSession, UUID> {

    Optional<WorkSession> findByBusinessIdAndUserIdAndClosedAtIsNull(UUID businessId, UUID userId);

    List<WorkSession> findByBusinessIdOrderByOpenedAtDesc(UUID businessId);
}
