package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.TradeDto;
import com.bryan.forge.billing.datamodel.Trade;
import com.bryan.forge.billing.datamodel.TradeLine;
import com.bryan.forge.billing.datamodel.TradeStatus;
import com.bryan.forge.billing.datarepository.TradeLineRepository;
import com.bryan.forge.billing.datarepository.TradeRepository;
import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.core.backend.AuditService;
import com.bryan.forge.core.backend.ForbiddenException;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.ledger.backend.LedgerService;
import com.bryan.forge.ledger.datamodel.MovementType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeServiceTest {

    private final TradeRepository tradeRepo = mock(TradeRepository.class);
    private final TradeLineRepository lineRepo = mock(TradeLineRepository.class);
    private final ItemRepository itemRepo = mock(ItemRepository.class);
    private final BusinessRepository businessRepo = mock(BusinessRepository.class);
    private final BusinessAccessService access = mock(BusinessAccessService.class);
    private final LedgerService ledger = mock(LedgerService.class);
    private final AuditService audit = mock(AuditService.class);
    @SuppressWarnings("unchecked")
    private final io.micronaut.context.event.ApplicationEventPublisher<Object> events =
            mock(io.micronaut.context.event.ApplicationEventPublisher.class);
    private final TradeService service = new TradeService(tradeRepo, lineRepo, itemRepo,
            businessRepo, access, ledger, audit, events);

    private final User actor = mock(User.class);
    private final UUID vendeur = UUID.randomUUID();
    private final UUID acheteur = UUID.randomUUID();
    private final UUID tid = UUID.randomUUID();
    private final UUID fer = UUID.randomUUID();
    private final UUID stockFrom = UUID.randomUUID();
    private final UUID stockTo = UUID.randomUUID();
    private final UUID coffreFrom = UUID.randomUUID();
    private final UUID coffreTo = UUID.randomUUID();

    private Trade proposedTrade(long septims) {
        Trade t = mock(Trade.class);
        when(t.getId()).thenReturn(tid);
        when(t.getNumero()).thenReturn(1L);
        when(t.getFromBusinessId()).thenReturn(vendeur);
        when(t.getToBusinessId()).thenReturn(acheteur);
        when(t.getStatus()).thenReturn(TradeStatus.PROPOSEE);
        when(t.getSeptims()).thenReturn(septims);
        return t;
    }

    private void setupBusinesses() {
        Business from = mock(Business.class);
        when(from.getId()).thenReturn(vendeur);
        when(from.getNom()).thenReturn("Forge A");
        when(from.getDefaultStockAccountId()).thenReturn(stockFrom);
        when(from.getDefaultCoffreAccountId()).thenReturn(coffreFrom);
        Business to = mock(Business.class);
        when(to.getId()).thenReturn(acheteur);
        when(to.getNom()).thenReturn("Compagnie B");
        when(to.getDefaultStockAccountId()).thenReturn(stockTo);
        when(to.getDefaultCoffreAccountId()).thenReturn(coffreTo);
        when(businessRepo.findById(vendeur)).thenReturn(Optional.of(from));
        when(businessRepo.findById(acheteur)).thenReturn(Optional.of(to));
    }

    @Test
    void accept_executeMarchandiseEtSeptimsDesDeuxCotes() {
        Trade t = proposedTrade(50);
        setupBusinesses();
        when(tradeRepo.findById(tid)).thenReturn(Optional.of(t));
        TradeLine line = mock(TradeLine.class);
        when(line.getItemId()).thenReturn(fer);
        when(line.getQuantity()).thenReturn(10);
        when(lineRepo.findByTradeId(tid)).thenReturn(List.of(line));
        Item septime = mock(Item.class);
        when(septime.getId()).thenReturn(UUID.randomUUID());
        when(itemRepo.findFirstBySystemTrue()).thenReturn(Optional.of(septime));
        when(itemRepo.findAll()).thenReturn(List.of());

        TradeDto dto = service.accept(actor, acheteur, tid);

        // Marchandise : OUT chez le vendeur, IN chez l'acheteur.
        verify(ledger).applyMovement(eq(vendeur), eq(fer), eq(10), eq(stockFrom), isNull(),
                eq(MovementType.TRANSFER), eq("TRADE"), eq(tid), any(), any());
        verify(ledger).applyMovement(eq(acheteur), eq(fer), eq(10), isNull(), eq(stockTo),
                eq(MovementType.TRANSFER), eq("TRADE"), eq(tid), any(), any());
        // Septims : OUT chez l'acheteur, IN chez le vendeur.
        verify(ledger).applyMovement(eq(acheteur), any(), eq(50), eq(coffreTo), isNull(),
                eq(MovementType.TRANSFER), eq("TRADE"), eq(tid), any(), any());
        verify(ledger).applyMovement(eq(vendeur), any(), eq(50), isNull(), eq(coffreFrom),
                eq(MovementType.TRANSFER), eq("TRADE"), eq(tid), any(), any());
        verify(ledger, times(4)).applyMovement(any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                any(), any(), any(), any(), any(), any(), any());
        verify(t).decide(TradeStatus.ACCEPTEE, actor.getId());
        assertThat(dto).isNotNull();
    }

    @Test
    void accept_refuseSiPasLeDestinataire() {
        Trade t = proposedTrade(0);
        when(tradeRepo.findById(tid)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.accept(actor, vendeur, tid))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancel_reserveAuVendeur() {
        Trade t = proposedTrade(0);
        when(tradeRepo.findById(tid)).thenReturn(Optional.of(t));
        when(itemRepo.findAll()).thenReturn(List.of());
        setupBusinesses();

        service.cancel(actor, vendeur, tid);
        verify(t).decide(TradeStatus.ANNULEE, actor.getId());

        assertThatThrownBy(() -> service.cancel(actor, acheteur, tid))
                .isInstanceOf(ForbiddenException.class);
    }
}
