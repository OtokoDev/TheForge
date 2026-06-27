package com.bryan.forge.catalog.backend;

import com.bryan.forge.catalog.backend.dto.CreateTaxonRequest;
import com.bryan.forge.catalog.backend.dto.ReorderRequest;
import com.bryan.forge.catalog.backend.dto.TaxonDto;
import com.bryan.forge.catalog.backend.dto.UpdateTaxonRequest;
import com.bryan.forge.catalog.datamodel.TaxonKind;
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

/** Familles d'items (épée, casque, lingot…). Lecture authentifiée ; écriture SYSTEM. */
@Controller("/api/catalog/families")
@ExecuteOn(TaskExecutors.BLOCKING)
public class FamilyController {

    private final TaxonService taxonService;

    public FamilyController(TaxonService taxonService) {
        this.taxonService = taxonService;
    }

    @Get
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public List<TaxonDto> list() {
        return taxonService.list(TaxonKind.FAMILY);
    }

    @Post
    @Status(HttpStatus.CREATED)
    @Secured("ROLE_SYSTEM")
    public TaxonDto create(@Body CreateTaxonRequest req) {
        return taxonService.create(TaxonKind.FAMILY, req.nom(), req.couleur());
    }

    @Put("/reorder")
    @Secured("ROLE_SYSTEM")
    public List<TaxonDto> reorder(@Body ReorderRequest req) {
        return taxonService.reorder(TaxonKind.FAMILY, req.ids());
    }

    @Put("/{id}")
    @Secured("ROLE_SYSTEM")
    public TaxonDto update(UUID id, @Body UpdateTaxonRequest req) {
        return taxonService.update(TaxonKind.FAMILY, id, req.nom(), req.ordre(), req.couleur(), req.version());
    }

    @Delete("/{id}")
    @Status(HttpStatus.NO_CONTENT)
    @Secured("ROLE_SYSTEM")
    public void delete(UUID id) {
        taxonService.delete(TaxonKind.FAMILY, id);
    }
}
