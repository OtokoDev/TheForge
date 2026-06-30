package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.CreateFactureLine;
import com.bryan.forge.billing.backend.dto.CreatePurchaseRequest;
import com.bryan.forge.billing.datamodel.Purchase;
import com.bryan.forge.billing.datamodel.PurchaseLine;
import com.bryan.forge.billing.datarepository.PurchaseLineRepository;
import com.bryan.forge.billing.datarepository.PurchaseRepository;
import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.ledger.backend.LedgerService;
import com.bryan.forge.ledger.datamodel.MovementType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseServiceTest {

    private final PurchaseRepository purchaseRepo = mock(PurchaseRepository.class);
    private final PurchaseLineRepository lineRepo = mock(PurchaseLineRepository.class);
    private final ItemRepository itemRepo = mock(ItemRepository.class);
    private final BusinessRepository businessRepo = mock(BusinessRepository.class);
    private final BusinessAccessService access = mock(BusinessAccessService.class);
    private final LedgerService ledgerService = mock(LedgerService.class);
    private final PurchaseService service = new PurchaseService(purchaseRepo, lineRepo, itemRepo,
            businessRepo, access, ledgerService);

    private final User actor = mock(User.class);
    private final UUID biz = UUID.randomUUID();
    private final UUID itemX = UUID.randomUUID();
    private final UUID coffre = UUID.randomUUID();
    private final UUID stock = UUID.randomUUID();
    private final UUID septimId = UUID.randomUUID();

    @Test
    void create_paieLeCoffreEtRemplitLeStock() {
        Business b = mock(Business.class);
        when(b.getDefaultCoffreAccountId()).thenReturn(coffre);
        when(b.getDefaultStockAccountId()).thenReturn(stock);
        when(businessRepo.findById(biz)).thenReturn(Optional.of(b));
        when(itemRepo.findById(itemX)).thenReturn(Optional.of(mock(Item.class)));
        Item septim = mock(Item.class);
        when(septim.getId()).thenReturn(septimId);
        when(itemRepo.findFirstBySystemTrue()).thenReturn(Optional.of(septim));
        when(itemRepo.findAll()).thenReturn(List.of());
        when(purchaseRepo.nextNumero()).thenReturn(1L);
        when(purchaseRepo.save(any(Purchase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lineRepo.findByPurchaseId(any())).thenReturn(List.of());
        when(actor.getId()).thenReturn(UUID.randomUUID());

        service.create(actor, biz, new CreatePurchaseRequest("Forge & Co",
                List.of(new CreateFactureLine(itemX, 5, new BigDecimal("10"))))); // total = 50

        // 50 septimes sortis du coffre
        verify(ledgerService).applyMovement(eq(biz), eq(septimId), eq(50), eq(coffre), isNull(),
                eq(MovementType.PURCHASE), eq("ACHAT"), any(), any(), any());
        // 5 matières entrées au stock
        verify(ledgerService).applyMovement(eq(biz), eq(itemX), eq(5), isNull(), eq(stock),
                eq(MovementType.PURCHASE), eq("ACHAT"), any(), any(), any());
        verify(lineRepo).save(any(PurchaseLine.class));
    }
}
