package com.bryan.forge.treasury.backend;

import com.bryan.forge.business.backend.CurrentUser;
import com.bryan.forge.treasury.backend.dto.CreanceEntryDto;
import com.bryan.forge.treasury.backend.dto.CreanceFarmerDto;
import com.bryan.forge.treasury.backend.dto.DepositRequest;
import com.bryan.forge.treasury.backend.dto.PaymentRequest;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.util.List;
import java.util.UUID;

/** Créances envers les farmeurs. Lecture : membre/staff. Dépôt/paiement : membre opérant. */
@Controller("/api/businesses/{businessId}/creances")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_AUTHENTICATED)
public class CreanceController {

    private final CreanceService creanceService;
    private final CurrentUser currentUser;

    public CreanceController(CreanceService creanceService, CurrentUser currentUser) {
        this.creanceService = creanceService;
        this.currentUser = currentUser;
    }

    @Get
    public List<CreanceFarmerDto> farmers(UUID businessId) {
        return creanceService.listFarmers(currentUser.require(), businessId);
    }

    @Get("/entries")
    public List<CreanceEntryDto> entries(UUID businessId, @QueryValue String farmerName) {
        return creanceService.entries(currentUser.require(), businessId, farmerName);
    }

    @Post("/deposit")
    public CreanceFarmerDto deposit(UUID businessId, @Body DepositRequest req) {
        return creanceService.deposit(currentUser.require(), businessId, req.farmerName(),
                req.lines(), req.stockAccountId(), req.reference());
    }

    @Post("/payment")
    public CreanceFarmerDto payment(UUID businessId, @Body PaymentRequest req) {
        return creanceService.pay(currentUser.require(), businessId, req.farmerName(),
                req.amount(), req.coffreAccountId(), req.reference());
    }
}
