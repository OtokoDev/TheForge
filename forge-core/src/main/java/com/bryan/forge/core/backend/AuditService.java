package com.bryan.forge.core.backend;

import com.bryan.forge.core.datamodel.ActivityLog;
import com.bryan.forge.core.datarepository.ActivityLogRepository;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

/** Journal d'activité (main courante), utilisable par tous les modules. */
@Singleton
public class AuditService {

    private final ActivityLogRepository repo;

    public AuditService(ActivityLogRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void record(UUID businessId, UUID userId, String action, String details) {
        repo.save(new ActivityLog(businessId, userId, action, details));
    }

    /** Événement global (sans business) : main courante SYSTEM. */
    @Transactional
    public void recordSystem(UUID userId, String action, String details) {
        repo.save(new ActivityLog(null, userId, action, details));
    }

    @Transactional
    public List<ActivityLog> list(UUID businessId) {
        return repo.findByBusinessIdOrderByCreatedAtDesc(businessId);
    }

    @Transactional
    public List<ActivityLog> list(UUID businessId, int limit) {
        return repo.findByBusinessIdOrderByCreatedAtDesc(businessId, Pageable.from(0, limit));
    }

    @Transactional
    public List<ActivityLog> listSystem(int limit) {
        return repo.findByBusinessIdIsNullOrderByCreatedAtDesc(Pageable.from(0, limit));
    }
}
