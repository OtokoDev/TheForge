package com.bryan.forge.business.backend;

import com.bryan.forge.business.backend.dto.BusinessDto;
import com.bryan.forge.business.backend.dto.LogoDto;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datamodel.BusinessType;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.business.datarepository.MembershipRepository;
import com.bryan.forge.core.datamodel.GlobalRole;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.security.backend.CurrentActor;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Singleton
public class BusinessService {

    private static final int MAX_LOGO_DATA_URL_LENGTH = 1_400_000; // ~1 Mo binaire (base64 +33 %)

    private final BusinessRepository businessRepo;
    private final MembershipRepository membershipRepo;
    private final BusinessAccessService access;
    private final CurrentActor currentActor;

    public BusinessService(BusinessRepository businessRepo, MembershipRepository membershipRepo,
                           BusinessAccessService access, CurrentActor currentActor) {
        this.businessRepo = businessRepo;
        this.membershipRepo = membershipRepo;
        this.access = access;
        this.currentActor = currentActor;
    }

    @Transactional
    public BusinessDto create(String nom, BusinessType type) {
        Business business = new Business(nom, type);
        UUID by = currentActor.stampId();
        business.setCreatedBy(by);
        business.setModifiedBy(by);
        return BusinessDto.from(businessRepo.save(business));
    }

    /**
     * Business visibles pour l'utilisateur : tous pour SYSTEM/STAFF (lecture globale),
     * sinon uniquement ceux où il a une appartenance.
     */
    @Transactional
    public List<BusinessDto> visibleFor(User user) {
        boolean global = user.getGlobalRole() == GlobalRole.SYSTEM
                || user.getGlobalRole() == GlobalRole.STAFF;

        List<Business> businesses = global
                ? businessRepo.findAll()
                : membershipRepo.findByUserId(user.getId()).stream()
                        .map(m -> m.getBusiness())
                        .toList();

        return businesses.stream().map(BusinessDto::from).toList();
    }

    @Transactional
    public LogoDto getLogo(User actor, UUID businessId) {
        Business business = require(businessId);
        access.requireView(actor, businessId);
        return new LogoDto(business.getLogoDataUrl());
    }

    /** Définit (ou efface si null) le logo du business. Réservé à l'ADMIN du business. */
    @Transactional
    public void setLogo(User actor, UUID businessId, String dataUrl) {
        Business business = require(businessId);
        access.requireAdmin(actor, businessId);
        if (dataUrl != null && !dataUrl.isBlank()) {
            if (!dataUrl.startsWith("data:image/")) {
                throw new IllegalArgumentException("Image invalide (data-URL image attendue)");
            }
            if (dataUrl.length() > MAX_LOGO_DATA_URL_LENGTH) {
                throw new IllegalArgumentException("Image trop lourde (max ~1 Mo)");
            }
        }
        business.setLogoDataUrl(dataUrl == null || dataUrl.isBlank() ? null : dataUrl);
        business.setModifiedBy(actor.getId());
        businessRepo.update(business);
    }

    private Business require(UUID businessId) {
        return businessRepo.findById(businessId)
                .orElseThrow(() -> new NoSuchElementException("Business introuvable : " + businessId));
    }
}
