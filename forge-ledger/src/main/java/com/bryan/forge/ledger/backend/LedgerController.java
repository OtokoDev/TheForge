package com.bryan.forge.ledger.backend;

import com.bryan.forge.business.backend.CurrentUser;
import com.bryan.forge.ledger.backend.dto.AccountDto;
import com.bryan.forge.ledger.backend.dto.CreateAccountRequest;
import com.bryan.forge.ledger.backend.dto.DefaultsDto;
import com.bryan.forge.ledger.backend.dto.SetDefaultsRequest;
import com.bryan.forge.ledger.backend.dto.InventoryRequest;
import com.bryan.forge.ledger.backend.dto.InventoryResultDto;
import com.bryan.forge.ledger.backend.dto.ItemBalanceDto;
import com.bryan.forge.ledger.backend.dto.MovementDto;
import com.bryan.forge.ledger.backend.dto.RecordMovementRequest;
import com.bryan.forge.ledger.backend.dto.StockRowDto;
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

/**
 * Comptes/coffres + journal de mouvements d'un business. Lecture : membre du business
 * (ou SYSTEM/STAFF). Création de compte : ADMIN. Saisie de mouvement : membre opérant
 * (ADMIN/MEMBRE, pas STAFF). L'autorisation fine est portée par le service.
 */
@Controller("/api/businesses/{businessId}")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_AUTHENTICATED)
public class LedgerController {

    private final LedgerService ledger;
    private final CurrentUser currentUser;

    public LedgerController(LedgerService ledger, CurrentUser currentUser) {
        this.ledger = ledger;
        this.currentUser = currentUser;
    }

    @Get("/accounts")
    public List<AccountDto> accounts(UUID businessId) {
        return ledger.listAccounts(currentUser.require(), businessId);
    }

    @Post("/accounts")
    @Status(HttpStatus.CREATED)
    public AccountDto createAccount(UUID businessId, @Body CreateAccountRequest req) {
        return ledger.createAccount(currentUser.require(), businessId, req.name(), req.kind());
    }

    @Delete("/accounts/{accountId}")
    @Status(HttpStatus.NO_CONTENT)
    public void deleteAccount(UUID businessId, UUID accountId) {
        ledger.deleteAccount(currentUser.require(), businessId, accountId);
    }

    @Get("/accounts/{accountId}/balances")
    public List<ItemBalanceDto> balances(UUID businessId, UUID accountId) {
        return ledger.balances(currentUser.require(), businessId, accountId);
    }

    @Get("/movements")
    public List<MovementDto> journal(UUID businessId) {
        return ledger.journal(currentUser.require(), businessId);
    }

    /** Stock agrégé (toutes lignes compte × item). */
    @Get("/stock")
    public List<StockRowDto> stock(UUID businessId) {
        return ledger.stock(currentUser.require(), businessId);
    }

    /** Validation d'inventaire (ADMIN) : régularise les écarts comptés. */
    @Post("/inventory")
    public InventoryResultDto inventory(UUID businessId, @Body InventoryRequest req) {
        return ledger.validateInventory(currentUser.require(), businessId, req.counts());
    }

    @Get("/defaults")
    public DefaultsDto defaults(UUID businessId) {
        return ledger.getDefaults(currentUser.require(), businessId);
    }

    /** Définit les comptes par défaut (ADMIN) pour le POS. */
    @Put("/defaults")
    public DefaultsDto setDefaults(UUID businessId, @Body SetDefaultsRequest req) {
        return ledger.setDefaults(currentUser.require(), businessId, req.stockAccountId(), req.coffreAccountId());
    }

    @Post("/movements")
    @Status(HttpStatus.CREATED)
    public MovementDto record(UUID businessId, @Body RecordMovementRequest req) {
        return ledger.record(currentUser.require(), businessId, req);
    }
}
