package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.TaxRateDto;
import com.bryan.forge.billing.backend.dto.TaxRateHistoryDto;
import com.bryan.forge.billing.datamodel.TaxBase;
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
                .map(t -> new TaxRateDto(t.getRate(), t.getBase(), t.getValidFrom()))
                .orElse(new TaxRateDto(BigDecimal.ZERO, TaxBase.PROFIT, null));
    }

    /** Taux courant pour usage interne (factures) ; 0 si non défini. */
    @Transactional
    public BigDecimal currentRate(UUID businessId) {
        return repo.findByBusinessIdAndValidToIsNull(businessId).map(TaxRate::getRate).orElse(BigDecimal.ZERO);
    }

    /** Assiette courante pour usage interne (factures) ; PROFIT si non défini. */
    @Transactional
    public TaxBase currentBase(UUID businessId) {
        return repo.findByBusinessIdAndValidToIsNull(businessId).map(TaxRate::getBase).orElse(TaxBase.PROFIT);
    }

    /** Définit le taux courant : clôture le précédent, en crée un nouveau (historique conservé). */
    @Transactional
    public TaxRateDto setRate(User actor, UUID businessId, BigDecimal rate, TaxBase base) {
        requireBusiness(businessId);
        access.requireAdmin(actor, businessId);
        if (rate == null || rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Le taux doit être compris entre 0 et 1");
        }
        repo.findByBusinessIdAndValidToIsNull(businessId).ifPresent(currentRate -> {
            currentRate.setValidTo(Instant.now());
            repo.update(currentRate);
            // Flush avant l'insert : Hibernate ordonne sinon INSERT avant UPDATE → 2 lignes
            // courantes → viole uq_tax_rate_current.
            repo.flush();
        });
        TaxRate taxRate = new TaxRate(businessId, rate, base == null ? TaxBase.PROFIT : base);
        taxRate.setCreatedBy(actor.getId());
        taxRate.setModifiedBy(actor.getId());
        TaxRate saved = repo.save(taxRate);
        return new TaxRateDto(saved.getRate(), saved.getBase(), saved.getValidFrom());
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
