package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.CommandeDto;
import com.bryan.forge.billing.backend.dto.CreateCommandeRequest;
import com.bryan.forge.billing.backend.dto.FactureDto;
import com.bryan.forge.billing.backend.dto.SetCommandeStatusRequest;
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
 * Commandes client d'un business. Lecture : membre/staff. Création / édition / statut /
 * conversion : membre opérant. Autorisation fine portée par le service.
 */
@Controller("/api/businesses/{businessId}/commandes")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_AUTHENTICATED)
public class CommandeController {

    private final CommandeService service;
    private final CurrentUser currentUser;

    public CommandeController(CommandeService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @Get
    public List<CommandeDto> list(UUID businessId) {
        return service.list(currentUser.require(), businessId);
    }

    @Post
    @Status(HttpStatus.CREATED)
    public CommandeDto create(UUID businessId, @Body CreateCommandeRequest req) {
        return service.create(currentUser.require(), businessId, req);
    }

    @Get("/{commandeId}")
    public CommandeDto get(UUID businessId, UUID commandeId) {
        return service.get(currentUser.require(), businessId, commandeId);
    }

    /** Met à jour un devis (lignes + client). */
    @Put("/{commandeId}")
    public CommandeDto update(UUID businessId, UUID commandeId, @Body CreateCommandeRequest req) {
        return service.update(currentUser.require(), businessId, commandeId, req);
    }

    @Put("/{commandeId}/status")
    public CommandeDto setStatus(UUID businessId, UUID commandeId, @Body SetCommandeStatusRequest req) {
        return service.setStatus(currentUser.require(), businessId, commandeId, req.status());
    }

    /** Livraison : génère la facture BROUILLON et marque la commande LIVREE. */
    @Post("/{commandeId}/facture")
    public FactureDto toFacture(UUID businessId, UUID commandeId) {
        return service.convertToFacture(currentUser.require(), businessId, commandeId);
    }

    @Delete("/{commandeId}")
    @Status(HttpStatus.NO_CONTENT)
    public void delete(UUID businessId, UUID commandeId) {
        service.delete(currentUser.require(), businessId, commandeId);
    }
}
