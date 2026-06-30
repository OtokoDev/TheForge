package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.PayRequest;
import com.bryan.forge.billing.datamodel.Facture;
import com.bryan.forge.billing.datamodel.FactureStatus;
import com.bryan.forge.billing.datamodel.Payout;
import com.bryan.forge.billing.datarepository.ExpenseRepository;
import com.bryan.forge.billing.datarepository.FactureRepository;
import com.bryan.forge.billing.datarepository.PayoutRepository;
import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.core.datarepository.UserRepository;
import com.bryan.forge.ledger.backend.LedgerService;
import com.bryan.forge.ledger.datamodel.MovementType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceServiceTest {

    private final FactureRepository factureRepo = mock(FactureRepository.class);
    private final PayoutRepository payoutRepo = mock(PayoutRepository.class);
    private final ExpenseRepository expenseRepo = mock(ExpenseRepository.class);
    private final ItemRepository itemRepo = mock(ItemRepository.class);
    private final BusinessRepository businessRepo = mock(BusinessRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final BusinessAccessService access = mock(BusinessAccessService.class);
    private final LedgerService ledgerService = mock(LedgerService.class);
    private final FinanceService service = new FinanceService(factureRepo, payoutRepo, expenseRepo,
            itemRepo, businessRepo, userRepo, access, ledgerService);

    private final User actor = mock(User.class);
    private final UUID biz = UUID.randomUUID();
    private final UUID forgeron = UUID.randomUUID();
    private final UUID coffre = UUID.randomUUID();
    private final UUID septimId = UUID.randomUUID();

    private void setupForgeronEarned(String workerShare) {
        Facture f = mock(Facture.class);
        when(f.getStatus()).thenReturn(FactureStatus.VALIDEE);
        when(f.getCreatedBy()).thenReturn(forgeron);
        when(f.getWorkerShare()).thenReturn(new BigDecimal(workerShare));
        when(factureRepo.findByBusinessIdOrderByNumeroDesc(biz)).thenReturn(List.of(f));
        when(payoutRepo.findByBusinessIdOrderByCreatedAtDesc(biz)).thenReturn(List.of());
        Business b = mock(Business.class);
        when(b.getDefaultCoffreAccountId()).thenReturn(coffre);
        when(businessRepo.findById(biz)).thenReturn(Optional.of(b));
        Item septim = mock(Item.class);
        when(septim.getId()).thenReturn(septimId);
        when(itemRepo.findFirstBySystemTrue()).thenReturn(Optional.of(septim));
        when(userRepo.findById(forgeron)).thenReturn(Optional.of(actor));
        when(actor.getDisplayName()).thenReturn("Bjorn");
        when(actor.getId()).thenReturn(UUID.randomUUID());
    }

    @Test
    void pay_verseDepuisLeCoffre() {
        setupForgeronEarned("100");
        when(payoutRepo.save(any(Payout.class))).thenAnswer(inv -> inv.getArgument(0));

        service.pay(actor, biz, new PayRequest(forgeron, 60, null));

        verify(ledgerService).applyMovement(eq(biz), eq(septimId), eq(60), eq(coffre), isNull(),
                eq(MovementType.WITHDRAWAL), eq("PAIE"), isNull(), any(), any());
        verify(payoutRepo).save(any(Payout.class));
    }

    @Test
    void pay_refuseSiSuperieurAuSoldeDu() {
        setupForgeronEarned("100");

        assertThatThrownBy(() -> service.pay(actor, biz, new PayRequest(forgeron, 200, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
