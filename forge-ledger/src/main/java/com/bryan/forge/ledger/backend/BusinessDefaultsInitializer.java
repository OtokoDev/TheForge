package com.bryan.forge.ledger.backend;

import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.business.event.BusinessCreatedEvent;
import com.bryan.forge.ledger.datamodel.Account;
import com.bryan.forge.ledger.datamodel.AccountKind;
import com.bryan.forge.ledger.datarepository.AccountRepository;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

/**
 * À la création d'un business : crée un compte « Coffre principal » (STOCK) et le définit
 * comme stock ET coffre par défaut → vente possible immédiatement. Écouteur synchrone,
 * exécuté DANS la transaction de création (atomique : si l'init échoue, le business ne
 * persiste pas). forge-ledger dépend de forge-business, d'où l'init côté ledger.
 */
@Singleton
public class BusinessDefaultsInitializer {

    private final AccountRepository accountRepo;
    private final BusinessRepository businessRepo;

    public BusinessDefaultsInitializer(AccountRepository accountRepo, BusinessRepository businessRepo) {
        this.accountRepo = accountRepo;
        this.businessRepo = businessRepo;
    }

    @EventListener
    @Transactional
    void onBusinessCreated(BusinessCreatedEvent e) {
        Business business = businessRepo.findById(e.businessId()).orElse(null);
        if (business == null || business.getDefaultStockAccountId() != null) {
            return; // business absent ou déjà initialisé
        }
        Account account = accountRepo.save(new Account(e.businessId(), "Coffre principal", AccountKind.STOCK));
        business.setDefaultStockAccountId(account.getId());
        business.setDefaultCoffreAccountId(account.getId());
        businessRepo.update(business);
    }
}
