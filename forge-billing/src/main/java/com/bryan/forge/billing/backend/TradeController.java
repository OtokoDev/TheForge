package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.CreateTradeRequest;
import com.bryan.forge.billing.backend.dto.TradeDto;
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

/** Commerce inter-business : propositions envoyées/reçues du business courant. */
@Controller("/api/businesses/{businessId}/trades")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_AUTHENTICATED)
public class TradeController {

    private final TradeService tradeService;
    private final CurrentUser currentUser;

    public TradeController(TradeService tradeService, CurrentUser currentUser) {
        this.tradeService = tradeService;
        this.currentUser = currentUser;
    }

    @Get
    public List<TradeDto> list(UUID businessId) {
        return tradeService.list(currentUser.require(), businessId);
    }

    @Post
    @Status(HttpStatus.CREATED)
    public TradeDto create(UUID businessId, @Body CreateTradeRequest req) {
        return tradeService.create(currentUser.require(), businessId, req);
    }

    @Post("/{id}/accept")
    public TradeDto accept(UUID businessId, UUID id) {
        return tradeService.accept(currentUser.require(), businessId, id);
    }

    @Post("/{id}/refuse")
    public TradeDto refuse(UUID businessId, UUID id) {
        return tradeService.refuse(currentUser.require(), businessId, id);
    }

    @Post("/{id}/cancel")
    public TradeDto cancel(UUID businessId, UUID id) {
        return tradeService.cancel(currentUser.require(), businessId, id);
    }
}
