package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.ItemCostDto;
import com.bryan.forge.billing.backend.dto.SetTaxRateRequest;
import com.bryan.forge.billing.backend.dto.TaxRateDto;
import com.bryan.forge.billing.backend.dto.TaxRateHistoryDto;
import com.bryan.forge.business.backend.CurrentUser;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Put;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.util.List;
import java.util.UUID;

/**
 * Taux de taxe (par business, historisé) + coût de revient. Lecture : membre/staff.
 * Modification du taux : ADMIN. Autorisation fine portée par les services.
 */
@Controller("/api/businesses/{businessId}")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_AUTHENTICATED)
public class BillingController {

    private final TaxRateService taxRateService;
    private final CostingService costingService;
    private final CurrentUser currentUser;

    public BillingController(TaxRateService taxRateService, CostingService costingService, CurrentUser currentUser) {
        this.taxRateService = taxRateService;
        this.costingService = costingService;
        this.currentUser = currentUser;
    }

    @Get("/tax-rate")
    public TaxRateDto taxRate(UUID businessId) {
        return taxRateService.current(currentUser.require(), businessId);
    }

    @Put("/tax-rate")
    public TaxRateDto setTaxRate(UUID businessId, @Body SetTaxRateRequest req) {
        return taxRateService.setRate(currentUser.require(), businessId, req.rate(), req.base());
    }

    @Get("/tax-rate/history")
    public List<TaxRateHistoryDto> taxHistory(UUID businessId) {
        return taxRateService.history(currentUser.require(), businessId);
    }

    @Get("/items/{itemId}/cost")
    public ItemCostDto itemCost(UUID businessId, UUID itemId) {
        return costingService.cost(currentUser.require(), businessId, itemId);
    }

    /** Coût de revient de tous les items (valeur du stock). */
    @Get("/costs")
    public List<ItemCostDto> costs(UUID businessId) {
        return costingService.costs(currentUser.require(), businessId);
    }
}
