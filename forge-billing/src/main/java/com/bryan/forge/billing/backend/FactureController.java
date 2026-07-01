package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.CreateFactureRequest;
import com.bryan.forge.billing.backend.dto.FactureDto;
import com.bryan.forge.billing.backend.dto.UpdateLineRequest;
import com.bryan.forge.billing.backend.dto.ValidateFactureRequest;
import com.bryan.forge.business.backend.CurrentUser;
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
 * Factures d'un business. Lecture : membre/staff. Création / édition / validation :
 * membre opérant (ADMIN/MEMBRE). Autorisation fine portée par le service.
 */
@Controller("/api/businesses/{businessId}/factures")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_AUTHENTICATED)
public class FactureController {

    private final FactureService factureService;
    private final CurrentUser currentUser;

    public FactureController(FactureService factureService, CurrentUser currentUser) {
        this.factureService = factureService;
        this.currentUser = currentUser;
    }

    @Get
    public List<FactureDto> list(UUID businessId) {
        return factureService.list(currentUser.require(), businessId);
    }

    @Post
    @Status(HttpStatus.CREATED)
    public FactureDto create(UUID businessId, @Body CreateFactureRequest req) {
        return factureService.create(currentUser.require(), businessId, req);
    }

    @Get("/{factureId}")
    public FactureDto get(UUID businessId, UUID factureId) {
        return factureService.get(currentUser.require(), businessId, factureId);
    }

    /** Édite un brouillon : remplace lignes + client (créateur ou admin). */
    @Put("/{factureId}")
    public FactureDto replace(UUID businessId, UUID factureId, @Body CreateFactureRequest req) {
        return factureService.replaceDraft(currentUser.require(), businessId, factureId, req);
    }

    /** Supprime un brouillon (créateur ou admin). */
    @Delete("/{factureId}")
    @Status(HttpStatus.NO_CONTENT)
    public void delete(UUID businessId, UUID factureId) {
        factureService.deleteDraft(currentUser.require(), businessId, factureId);
    }

    @Put("/{factureId}/lines/{lineId}")
    public FactureDto updateLine(UUID businessId, UUID factureId, UUID lineId, @Body UpdateLineRequest req) {
        return factureService.updateLine(currentUser.require(), businessId, factureId, lineId,
                req.unitPrice(), req.quantity());
    }

    @Post("/{factureId}/validate")
    public FactureDto validate(UUID businessId, UUID factureId, @Body ValidateFactureRequest req) {
        return factureService.validate(currentUser.require(), businessId, factureId,
                req.paid(), req.stockAccountId(), req.coffreAccountId());
    }

    /** Avoir : annule une facture validée en inversant ses mouvements (ADMIN). */
    @Post("/{factureId}/cancel")
    public FactureDto cancel(UUID businessId, UUID factureId) {
        return factureService.cancel(currentUser.require(), businessId, factureId);
    }

    /** Encaisse une facture à crédit (septims → coffre par défaut). */
    @Post("/{factureId}/pay")
    public FactureDto pay(UUID businessId, UUID factureId) {
        return factureService.markPaid(currentUser.require(), businessId, factureId, null);
    }
}
