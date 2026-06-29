package com.bryan.forge.business.backend;

import com.bryan.forge.business.backend.dto.ActivityDto;
import com.bryan.forge.core.backend.AuditService;
import com.bryan.forge.core.datamodel.ActivityLog;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.core.datarepository.UserRepository;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Main courante du business (journal d'activité). Lecture : membre/staff. */
@Controller("/api/businesses/{businessId}/activity")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_AUTHENTICATED)
public class ActivityController {

    private final AuditService auditService;
    private final BusinessAccessService access;
    private final CurrentUser currentUser;
    private final UserRepository userRepo;

    public ActivityController(AuditService auditService, BusinessAccessService access,
                             CurrentUser currentUser, UserRepository userRepo) {
        this.auditService = auditService;
        this.access = access;
        this.currentUser = currentUser;
        this.userRepo = userRepo;
    }

    @Get
    public List<ActivityDto> list(UUID businessId, @Nullable @QueryValue Integer limit) {
        access.requireView(currentUser.require(), businessId);
        Map<UUID, String> names = new HashMap<>();
        return auditService.list(businessId, cap(limit)).stream()
                .map(e -> new ActivityDto(
                        e.getAction(), e.getDetails(),
                        e.getUserId() == null ? "système"
                                : names.computeIfAbsent(e.getUserId(), id -> userRepo.findById(id).map(User::getDisplayName).orElse("?")),
                        e.getCreatedAt()))
                .toList();
    }

    static int cap(Integer limit) {
        return limit == null ? 200 : Math.min(Math.max(limit, 1), 1000);
    }
}
