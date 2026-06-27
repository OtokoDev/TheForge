package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.SessionDto;
import com.bryan.forge.billing.backend.dto.ShiftStatusDto;
import com.bryan.forge.billing.datamodel.Facture;
import com.bryan.forge.billing.datamodel.FactureStatus;
import com.bryan.forge.billing.datamodel.WorkSession;
import com.bryan.forge.billing.datarepository.FactureRepository;
import com.bryan.forge.billing.datarepository.SessionRepository;
import com.bryan.forge.billing.event.ShiftClosedEvent;
import com.bryan.forge.billing.event.ShiftOpenedEvent;
import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.core.datamodel.User;
import io.micronaut.context.event.ApplicationEventPublisher;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Singleton
public class SessionService {

    private final SessionRepository sessionRepo;
    private final FactureRepository factureRepo;
    private final BusinessRepository businessRepo;
    private final BusinessAccessService access;
    private final TaxRateService taxRateService;
    private final ApplicationEventPublisher<Object> events;

    public SessionService(SessionRepository sessionRepo, FactureRepository factureRepo,
                          BusinessRepository businessRepo, BusinessAccessService access,
                          TaxRateService taxRateService, ApplicationEventPublisher<Object> events) {
        this.sessionRepo = sessionRepo;
        this.factureRepo = factureRepo;
        this.businessRepo = businessRepo;
        this.access = access;
        this.taxRateService = taxRateService;
        this.events = events;
    }

    /** Ouvre un poste pour l'utilisateur courant (un seul poste ouvert par user/business). */
    @Transactional
    public SessionDto open(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireOperate(actor, businessId);
        if (sessionRepo.findByBusinessIdAndUserIdAndClosedAtIsNull(businessId, actor.getId()).isPresent()) {
            throw new IllegalStateException("Un poste est déjà ouvert");
        }
        WorkSession session = sessionRepo.save(
                new WorkSession(businessId, actor.getId(), taxRateService.currentRate(businessId)));

        String businessName = businessRepo.findById(businessId).map(Business::getNom).orElse("?");
        events.publishEvent(new ShiftOpenedEvent(actor.getUsername(), actor.isWebhooksEnabled(),
                businessName, session.getOpenedAt()));
        return SessionDto.from(session);
    }

    /** Ferme le poste ouvert de l'utilisateur et fige le récap (agrégat des factures validées). */
    @Transactional
    public SessionDto close(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireOperate(actor, businessId);
        WorkSession session = sessionRepo.findByBusinessIdAndUserIdAndClosedAtIsNull(businessId, actor.getId())
                .orElseThrow(() -> new IllegalStateException("Aucun poste ouvert"));

        List<Facture> factures = factureRepo.findBySessionId(session.getId()).stream()
                .filter(f -> f.getStatus() == FactureStatus.VALIDEE)
                .toList();

        long totalSales = factures.stream().mapToLong(Facture::getTotalAmount).sum();
        BigDecimal totalCost = sum(factures, Facture::getTotalCost);
        BigDecimal totalProfit = sum(factures, Facture::getTotalProfit);
        BigDecimal businessShare = sum(factures, Facture::getBusinessShare);
        BigDecimal workerShare = sum(factures, Facture::getWorkerShare);

        session.close(factures.size(), totalSales, totalCost, totalProfit, businessShare, workerShare);
        WorkSession closed = sessionRepo.update(session);

        events.publishEvent(new ShiftClosedEvent(actor.getUsername(), actor.isWebhooksEnabled(),
                closed.getOrdersCount(), closed.getTotalSales(), closed.getTotalProfit(),
                closed.getBusinessShare(), closed.getWorkerShare(), closed.getOpenedAt(), closed.getClosedAt()));
        return SessionDto.from(closed);
    }

    /** Poste courant de l'utilisateur (pour l'indicateur "service en cours"). */
    @Transactional
    public ShiftStatusDto current(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        return sessionRepo.findByBusinessIdAndUserIdAndClosedAtIsNull(businessId, actor.getId())
                .map(s -> new ShiftStatusDto(true, SessionDto.from(s)))
                .orElse(new ShiftStatusDto(false, null));
    }

    @Transactional
    public List<SessionDto> history(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        return sessionRepo.findByBusinessIdOrderByOpenedAtDesc(businessId).stream().map(SessionDto::from).toList();
    }

    private static BigDecimal sum(List<Facture> factures, java.util.function.Function<Facture, BigDecimal> field) {
        return factures.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void requireBusiness(UUID businessId) {
        if (businessRepo.findById(businessId).isEmpty()) {
            throw new NoSuchElementException("Business introuvable : " + businessId);
        }
    }
}
