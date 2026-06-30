package com.bryan.forge.ledger.backend;

import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.business.event.BusinessCreatedEvent;
import com.bryan.forge.ledger.datamodel.Account;
import com.bryan.forge.ledger.datarepository.AccountRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessDefaultsInitializerTest {

    private final AccountRepository accountRepo = mock(AccountRepository.class);
    private final BusinessRepository businessRepo = mock(BusinessRepository.class);
    private final BusinessDefaultsInitializer init = new BusinessDefaultsInitializer(accountRepo, businessRepo);

    @Test
    void creeCoffrePrincipalEtDefinitLesDefauts() {
        UUID bizId = UUID.randomUUID();
        UUID accId = UUID.randomUUID();
        Business b = mock(Business.class);
        when(b.getDefaultStockAccountId()).thenReturn(null);
        Account acc = mock(Account.class);
        when(acc.getId()).thenReturn(accId);
        when(businessRepo.findById(bizId)).thenReturn(Optional.of(b));
        when(accountRepo.save(any(Account.class))).thenReturn(acc);

        init.onBusinessCreated(new BusinessCreatedEvent(bizId));

        verify(accountRepo).save(any(Account.class));
        verify(b).setDefaultStockAccountId(accId);
        verify(b).setDefaultCoffreAccountId(accId);
        verify(businessRepo).update(b);
    }

    @Test
    void neReinitialisePasSiDejaConfigure() {
        UUID bizId = UUID.randomUUID();
        Business b = mock(Business.class);
        when(b.getDefaultStockAccountId()).thenReturn(UUID.randomUUID()); // déjà un compte
        when(businessRepo.findById(bizId)).thenReturn(Optional.of(b));

        init.onBusinessCreated(new BusinessCreatedEvent(bizId));

        verify(accountRepo, never()).save(any());
        verify(businessRepo, never()).update(any());
    }
}
