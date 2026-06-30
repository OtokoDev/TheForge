package com.bryan.forge.business.backend;

import com.bryan.forge.business.backend.dto.CreateMapPointRequest;
import com.bryan.forge.business.backend.dto.MapPointDto;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Status;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.util.List;
import java.util.UUID;

/** Points d'intérêt de la carte (business COMPAGNIE). */
@Controller("/api/businesses/{businessId}/map-points")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_AUTHENTICATED)
public class MapPointController {

    private final MapPointService service;
    private final CurrentUser currentUser;

    public MapPointController(MapPointService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @Get
    public List<MapPointDto> list(UUID businessId) {
        return service.list(currentUser.require(), businessId);
    }

    @Post
    @Status(HttpStatus.CREATED)
    public MapPointDto create(UUID businessId, @Body CreateMapPointRequest req) {
        return service.create(currentUser.require(), businessId, req);
    }

    @Delete("/{id}")
    @Status(HttpStatus.NO_CONTENT)
    public void delete(UUID businessId, UUID id) {
        service.delete(currentUser.require(), businessId, id);
    }
}
