package com.bryan.forge.business.backend;

import com.bryan.forge.business.backend.dto.CreateMarkerTypeRequest;
import com.bryan.forge.business.backend.dto.MapMarkerTypeDto;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datamodel.BusinessType;
import com.bryan.forge.business.datamodel.MapMarkerType;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.business.datarepository.MapMarkerTypeRepository;
import com.bryan.forge.core.backend.ForbiddenException;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.core.realtime.RealtimeEvent;
import io.micronaut.context.event.ApplicationEventPublisher;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/** Types de marqueurs configurables (par compagnie). Écriture = ADMIN ; diffuse RealtimeEvent "MAP". */
@Singleton
public class MapMarkerTypeService {

    private static final int MAX_IMG_LENGTH = 1_400_000; // ~1 Mo (data URL base64)

    private final MapMarkerTypeRepository repo;
    private final BusinessRepository businessRepo;
    private final BusinessAccessService access;
    private final ApplicationEventPublisher<Object> events;

    public MapMarkerTypeService(MapMarkerTypeRepository repo, BusinessRepository businessRepo,
                                BusinessAccessService access, ApplicationEventPublisher<Object> events) {
        this.repo = repo;
        this.businessRepo = businessRepo;
        this.access = access;
        this.events = events;
    }

    @Transactional
    public List<MapMarkerTypeDto> list(User actor, UUID businessId) {
        requireCompagnie(businessId);
        access.requireView(actor, businessId);
        return repo.findByBusinessId(businessId).stream().map(MapMarkerTypeDto::from).toList();
    }

    @Transactional
    public MapMarkerTypeDto create(User actor, UUID businessId, CreateMarkerTypeRequest req) {
        requireCompagnie(businessId);
        access.requireAdmin(actor, businessId);
        if (req.label() == null || req.label().isBlank()) {
            throw new IllegalArgumentException("Libellé requis");
        }
        if (req.color() == null || req.color().isBlank()) {
            throw new IllegalArgumentException("Couleur requise");
        }
        String img = req.imageDataUrl();
        if (img != null && !img.isBlank()) {
            if (!img.startsWith("data:image/")) {
                throw new IllegalArgumentException("Image invalide (data-URL image attendue)");
            }
            if (img.length() > MAX_IMG_LENGTH) {
                throw new IllegalArgumentException("Image trop lourde (max ~1 Mo)");
            }
        } else {
            img = null;
        }
        MapMarkerType saved = repo.save(new MapMarkerType(businessId, req.label().trim(), req.color().trim(), img));
        events.publishEvent(new RealtimeEvent(businessId, "MAP"));
        return MapMarkerTypeDto.from(saved);
    }

    @Transactional
    public void delete(User actor, UUID businessId, UUID id) {
        requireCompagnie(businessId);
        access.requireAdmin(actor, businessId);
        MapMarkerType t = repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Type introuvable : " + id));
        if (!t.getBusinessId().equals(businessId)) {
            throw new ForbiddenException("Ce type n'appartient pas au business");
        }
        repo.delete(t);
        events.publishEvent(new RealtimeEvent(businessId, "MAP"));
    }

    private void requireCompagnie(UUID businessId) {
        Business b = businessRepo.findById(businessId)
                .orElseThrow(() -> new NoSuchElementException("Business introuvable : " + businessId));
        if (b.getType() != BusinessType.COMPAGNIE) {
            throw new ForbiddenException("Carte réservée aux compagnies");
        }
    }
}
