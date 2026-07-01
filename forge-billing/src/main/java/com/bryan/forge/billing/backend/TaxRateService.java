package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.TaxRateDto;
import com.bryan.forge.billing.backend.dto.TaxRateHistoryDto;
import com.bryan.forge.billing.datamodel.TaxRate;
import com.bryan.forge.billing.datarepository.TaxRateRepository;
import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.core.datamodel.User;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Singleton
public class TaxRateService {

    private final TaxRateRepository repo;
    private final BusinessRepository businessRepo;
    private final BusinessAccessService access;

    public TaxRateService(TaxRateRepository repo, BusinessRepository businessRepo, BusinessAccessService access) {
        this.repo = repo;
        this.businessRepo = businessRepo;
        this.access = access;
    }

    @Transactional
    public TaxRateDto current(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        return repo.findByBusinessIdAndValidToIsNull(businessId)
                .map(t -> new TaxRateDto(t.getRate(), t.getCityFixed(), t.getCityRate(), t.getValidFrom()))
                .orElse(new TaxRateDto(BigDecimal.ZERO, 0L, BigDecimal.ZERO, null));
    }

    /** Taux courant pour usage interne (factures) ; 0 si non défini. */
    @Transactional
    public BigDecimal currentRate(UUID businessId) {
        return repo.findByBusinessIdAndValidToIsNull(businessId).map(TaxRate::getRate).orElse(BigDecimal.ZERO);
    }

    /** Taxe ville — forfait hebdo courant (0 si non défini). */
    @Transactional
    public long currentCityFixed(UUID businessId) {
        return repo.findByBusinessIdAndValidToIsNull(businessId).map(TaxRate::getCityFixed).orElse(0L);
    }

    /** Taxe ville — taux courant sur le CA après paie forgerons (0 si non défini). */
    @Transactional
    public BigDecimal currentCityRate(UUID businessId) {
        return repo.findByBusinessIdAndValidToIsNull(businessId).map(TaxRate::getCityRate).orElse(BigDecimal.ZERO);
    }

    /** Définit part forgeron + taxe ville : clôture le précédent, en crée un nouveau (historique conservé). */
    @Transactional
    public TaxRateDto setRate(User actor, UUID businessId, BigDecimal rate, long cityFixed, BigDecimal cityRate) {
        requireBusiness(businessId);
        access.requireAdmin(actor, businessId);
        if (rate == null || rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("La part forgeron doit être comprise entre 0 et 1");
        }
        if (cityRate == null || cityRate.signum() < 0 || cityRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Le taux de taxe ville doit être compris entre 0 et 1");
        }
        if (cityFixed < 0) {
            throw new IllegalArgumentException("Le forfait de taxe ville doit être positif");
        }
        repo.findByBusinessIdAndValidToIsNull(businessId).ifPresent(currentRate -> {
            currentRate.setValidTo(Instant.now());
            repo.update(currentRate);
            // Flush avant l'insert : Hibernate ordonne sinon INSERT avant UPDATE → 2 lignes
            // courantes → viole uq_tax_rate_current.
            repo.flush();
        });
        TaxRate taxRate = new TaxRate(businessId, rate, cityFixed, cityRate);
        taxRate.setCreatedBy(actor.getId());
        taxRate.setModifiedBy(actor.getId());
        TaxRate saved = repo.save(taxRate);
        return new TaxRateDto(saved.getRate(), saved.getCityFixed(), saved.getCityRate(), saved.getValidFrom());
    }

    @Transactional
    public List<TaxRateHistoryDto> history(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        return repo.findByBusinessIdOrderByValidFromDesc(businessId).stream().map(TaxRateHistoryDto::from).toList();
    }

    private void requireBusiness(UUID businessId) {
        if (businessRepo.findById(businessId).isEmpty()) {
            throw new NoSuchElementException("Business introuvable : " + businessId);
        }
    }
}
