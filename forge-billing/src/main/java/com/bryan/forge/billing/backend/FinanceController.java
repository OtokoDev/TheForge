package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.CreateExpenseRequest;
import com.bryan.forge.billing.backend.dto.ExpenseDto;
import com.bryan.forge.billing.backend.dto.FinanceSummaryDto;
import com.bryan.forge.billing.backend.dto.ForgeronOwedDto;
import com.bryan.forge.billing.backend.dto.PayRequest;
import com.bryan.forge.billing.backend.dto.PayoutDto;
import com.bryan.forge.business.backend.CurrentUser;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.util.List;
import java.util.UUID;

/** Finance d'un business : paie des forgerons, dépenses, compte de résultat. */
@Controller("/api/businesses/{businessId}/finance")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_AUTHENTICATED)
public class FinanceController {

    private final FinanceService service;
    private final CurrentUser currentUser;

    public FinanceController(FinanceService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @Get("/owed")
    public List<ForgeronOwedDto> owed(UUID businessId) {
        return service.owed(currentUser.require(), businessId);
    }

    @Post("/pay")
    public PayoutDto pay(UUID businessId, @Body PayRequest req) {
        return service.pay(currentUser.require(), businessId, req);
    }

    @Get("/payouts")
    public List<PayoutDto> payouts(UUID businessId) {
        return service.payouts(currentUser.require(), businessId);
    }

    @Get("/expenses")
    public List<ExpenseDto> expenses(UUID businessId) {
        return service.expenses(currentUser.require(), businessId);
    }

    @Post("/expenses")
    public ExpenseDto addExpense(UUID businessId, @Body CreateExpenseRequest req) {
        return service.addExpense(currentUser.require(), businessId, req);
    }

    @Get("/summary")
    public FinanceSummaryDto summary(UUID businessId) {
        return service.summary(currentUser.require(), businessId);
    }
}
