package com.bryan.forge.business.backend;

import com.bryan.forge.business.backend.dto.CreateMapPointRequest;
import com.bryan.forge.business.backend.dto.MapPointDto;
import com.bryan.forge.business.backend.dto.UpdateMapPointRequest;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datamodel.BusinessType;
import com.bryan.forge.business.datamodel.MapPoint;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.business.datarepository.MapPointRepository;
import com.bryan.forge.core.backend.ForbiddenException;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.core.realtime.RealtimeEvent;
import io.micronaut.context.event.ApplicationEventPublisher;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Points d'intérêt RP de la carte. Réservé aux business COMPAGNIE. Toute écriture publie un
 * RealtimeEvent "MAP" → mise à jour live des autres clients abonnés au business.
 */
@Singleton
public class MapPointService {

    private final MapPointRepository repo;
    private final BusinessRepository businessRepo;
    private final BusinessAccessService access;
    private final ApplicationEventPublisher<Object> events;

    public MapPointService(MapPointRepository repo, BusinessRepository businessRepo,
                           BusinessAccessService access, ApplicationEventPublisher<Object> events) {
        this.repo = repo;
        this.businessRepo = businessRepo;
        this.access = access;
        this.events = events;
    }

    @Transactional
    public List<MapPointDto> list(User actor, UUID businessId) {
        requireCompagnie(businessId);
        access.requireView(actor, businessId);
        return repo.findByBusinessId(businessId).stream().map(MapPointDto::from).toList();
    }

    @Transactional
    public MapPointDto create(User actor, UUID businessId, CreateMapPointRequest req) {
        requireCompagnie(businessId);
        access.requireOperate(actor, businessId);
        if (req.label() == null || req.label().isBlank()) {
            throw new IllegalArgumentException("Libellé requis");
        }
        if (req.type() == null || req.type().isBlank()) {
            throw new IllegalArgumentException("Type requis");
        }
        String note = req.note() == null || req.note().isBlank() ? null : req.note().trim();
        MapPoint saved = repo.save(new MapPoint(businessId, req.type().trim(), req.label().trim(),
                req.x(), req.y(), note, actor.getId()));
        events.publishEvent(new RealtimeEvent(businessId, "MAP"));
        return MapPointDto.from(saved);
    }

    @Transactional
    public MapPointDto update(User actor, UUID businessId, UUID id, UpdateMapPointRequest req) {
        requireCompagnie(businessId);
        access.requireOperate(actor, businessId);
        MapPoint p = repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Point introuvable : " + id));
        if (!p.getBusinessId().equals(businessId)) {
            throw new ForbiddenException("Ce point n'appartient pas au business");
        }
        if (req.label() != null && !req.label().isBlank()) p.setLabel(req.label().trim());
        if (req.type() != null && !req.type().isBlank()) p.setType(req.type().trim());
        p.setNote(req.note() == null || req.note().isBlank() ? null : req.note().trim());
        repo.update(p);
        events.publishEvent(new RealtimeEvent(businessId, "MAP"));
        return MapPointDto.from(p);
    }

    @Transactional
    public void delete(User actor, UUID businessId, UUID id) {
        requireCompagnie(businessId);
        access.requireOperate(actor, businessId);
        MapPoint p = repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Point introuvable : " + id));
        if (!p.getBusinessId().equals(businessId)) {
            throw new ForbiddenException("Ce point n'appartient pas au business");
        }
        repo.delete(p);
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
