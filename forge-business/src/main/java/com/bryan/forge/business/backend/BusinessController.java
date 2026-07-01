package com.bryan.forge.business.backend;

import com.bryan.forge.business.backend.dto.AddMemberRequest;
import com.bryan.forge.business.backend.dto.BusinessDto;
import com.bryan.forge.business.backend.dto.CreateBusinessRequest;
import com.bryan.forge.business.backend.dto.LogoDto;
import com.bryan.forge.business.backend.dto.MemberDto;
import com.bryan.forge.business.backend.dto.SetHiddenScreensRequest;
import com.bryan.forge.business.backend.dto.SetLogoRequest;
import com.bryan.forge.business.backend.dto.WebhookUrlDto;
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

@Controller("/api/businesses")
@ExecuteOn(TaskExecutors.BLOCKING)
public class BusinessController {

    private final BusinessService businessService;
    private final MembershipService membershipService;
    private final CurrentUser currentUser;

    public BusinessController(BusinessService businessService, MembershipService membershipService,
                             CurrentUser currentUser) {
        this.businessService = businessService;
        this.membershipService = membershipService;
        this.currentUser = currentUser;
    }

    /** Business visibles par l'utilisateur courant (tous pour SYSTEM/STAFF, sinon les siens). */
    @Get
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public List<BusinessDto> list() {
        return businessService.visibleFor(currentUser.require());
    }

    /** Création d'un business : réservé au rôle global SYSTEM. */
    @Post
    @Status(HttpStatus.CREATED)
    @Secured("ROLE_SYSTEM")
    public BusinessDto create(@Body CreateBusinessRequest req) {
        return businessService.create(req.nom(), req.type());
    }

    @Get("/{id}/members")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public List<MemberDto> members(UUID id) {
        return membershipService.listMembers(currentUser.require(), id);
    }

    /** Ajoute/Met à jour un membre : SYSTEM ou ADMIN du business. */
    @Post("/{id}/members")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public MemberDto addMember(UUID id, @Body AddMemberRequest req) {
        return membershipService.addOrUpdateMember(currentUser.require(), id, req.userId(), req.role(), req.version());
    }

    /** Retire un membre : SYSTEM ou ADMIN du business. */
    @Delete("/{id}/members/{userId}")
    @Status(HttpStatus.NO_CONTENT)
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public void removeMember(UUID id, UUID userId) {
        membershipService.removeMember(currentUser.require(), id, userId);
    }

    /** Définit les écrans masqués (front) d'un business : réservé au rôle global SYSTEM. */
    @Put("/{id}/hidden-screens")
    @Secured("ROLE_SYSTEM")
    public BusinessDto setHiddenScreens(UUID id, @Body SetHiddenScreensRequest req) {
        return businessService.setHiddenScreens(id, req.screens());
    }

    /** Annuaire de tous les business (cible d'un échange inter-business). */
    @Get("/directory")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public List<BusinessDto> directory() {
        return businessService.directory();
    }

    /** Webhook Discord du business (lecture/écriture ADMIN). */
    @Get("/{id}/webhook")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public WebhookUrlDto webhook(UUID id) {
        return new WebhookUrlDto(businessService.getWebhookUrl(currentUser.require(), id));
    }

    @Put("/{id}/webhook")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public WebhookUrlDto setWebhook(UUID id, @Body WebhookUrlDto req) {
        businessService.setWebhookUrl(currentUser.require(), id, req.url());
        return new WebhookUrlDto(businessService.getWebhookUrl(currentUser.require(), id));
    }

    @Get("/{id}/logo")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public LogoDto logo(UUID id) {
        return businessService.getLogo(currentUser.require(), id);
    }

    /** Définit/efface le logo : SYSTEM ou ADMIN du business. */
    @Put("/{id}/logo")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public LogoDto setLogo(UUID id, @Body SetLogoRequest req) {
        businessService.setLogo(currentUser.require(), id, req.dataUrl());
        return businessService.getLogo(currentUser.require(), id);
    }
}
