package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.CreatePurchaseRequest;
import com.bryan.forge.billing.backend.dto.PurchaseDto;
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

/** Achats fournisseurs d'un business. */
@Controller("/api/businesses/{businessId}/purchases")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_AUTHENTICATED)
public class PurchaseController {

    private final PurchaseService service;
    private final CurrentUser currentUser;

    public PurchaseController(PurchaseService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @Get
    public List<PurchaseDto> list(UUID businessId) {
        return service.list(currentUser.require(), businessId);
    }

    @Post
    @Status(HttpStatus.CREATED)
    public PurchaseDto create(UUID businessId, @Body CreatePurchaseRequest req) {
        return service.create(currentUser.require(), businessId, req);
    }
}
