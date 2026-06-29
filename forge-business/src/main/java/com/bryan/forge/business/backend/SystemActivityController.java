package com.bryan.forge.business.backend;

import com.bryan.forge.business.backend.dto.ActivityDto;
import com.bryan.forge.core.backend.AuditService;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.core.datarepository.UserRepository;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Main courante SYSTEM (événements globaux : connexions, familles, membres…). */
@Controller("/api/system/activity")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured("ROLE_SYSTEM")
public class SystemActivityController {

    private final AuditService auditService;
    private final UserRepository userRepo;

    public SystemActivityController(AuditService auditService, UserRepository userRepo) {
        this.auditService = auditService;
        this.userRepo = userRepo;
    }

    @Get
    public List<ActivityDto> list(@Nullable @QueryValue Integer limit) {
        Map<UUID, String> names = new HashMap<>();
        return auditService.listSystem(ActivityController.cap(limit)).stream()
                .map(e -> new ActivityDto(
                        e.getAction(), e.getDetails(),
                        e.getUserId() == null ? "anonyme"
                                : names.computeIfAbsent(e.getUserId(), id -> userRepo.findById(id).map(User::getDisplayName).orElse("?")),
                        e.getCreatedAt()))
                .toList();
    }
}
