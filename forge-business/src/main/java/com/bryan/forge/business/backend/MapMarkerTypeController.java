package com.bryan.forge.business.backend;

import com.bryan.forge.business.backend.dto.CreateMarkerTypeRequest;
import com.bryan.forge.business.backend.dto.MapMarkerTypeDto;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.Status;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.util.List;
import java.util.UUID;

/** Types de marqueurs de la carte (business COMPAGNIE). */
@Controller("/api/businesses/{businessId}/marker-types")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_AUTHENTICATED)
public class MapMarkerTypeController {

    private final MapMarkerTypeService service;
    private final CurrentUser currentUser;

    public MapMarkerTypeController(MapMarkerTypeService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @Get
    public List<MapMarkerTypeDto> list(UUID businessId) {
        return service.list(currentUser.require(), businessId);
    }

    @Post
    @Status(HttpStatus.CREATED)
    public MapMarkerTypeDto create(UUID businessId, @Body CreateMarkerTypeRequest req) {
        return service.create(currentUser.require(), businessId, req);
    }

    @Put("/{id}")
    public MapMarkerTypeDto update(UUID businessId, UUID id, @Body CreateMarkerTypeRequest req) {
        return service.update(currentUser.require(), businessId, id, req);
    }

    @Delete("/{id}")
    @Status(HttpStatus.NO_CONTENT)
    public void delete(UUID businessId, UUID id) {
        service.delete(currentUser.require(), businessId, id);
    }
}
