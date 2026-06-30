package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.CreateProductionRequest;
import com.bryan.forge.billing.backend.dto.ProductionOrderDto;
import com.bryan.forge.business.backend.CurrentUser;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Status;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.util.List;
import java.util.UUID;

/** Ordres de fabrication (atelier) d'un business. */
@Controller("/api/businesses/{businessId}/production")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_AUTHENTICATED)
public class ProductionController {

    private final ProductionService service;
    private final CurrentUser currentUser;

    public ProductionController(ProductionService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @Get
    public List<ProductionOrderDto> list(UUID businessId) {
        return service.list(currentUser.require(), businessId);
    }

    @Post
    @Status(HttpStatus.CREATED)
    public ProductionOrderDto create(UUID businessId, @Body CreateProductionRequest req) {
        return service.create(currentUser.require(), businessId, req);
    }

    @Post("/{id}/start")
    public ProductionOrderDto start(UUID businessId, UUID id) {
        return service.start(currentUser.require(), businessId, id);
    }

    /** Clôture l'ordre : consomme les ingrédients, fait entrer l'objet produit dans le stock. */
    @Post("/{id}/complete")
    public ProductionOrderDto complete(UUID businessId, UUID id) {
        return service.complete(currentUser.require(), businessId, id);
    }

    @Post("/{id}/cancel")
    public ProductionOrderDto cancel(UUID businessId, UUID id) {
        return service.cancel(currentUser.require(), businessId, id);
    }
}
