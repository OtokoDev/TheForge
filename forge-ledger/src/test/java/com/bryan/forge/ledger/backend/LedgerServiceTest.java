package com.bryan.forge.ledger.backend;

import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.ledger.backend.dto.ItemBalanceDto;
import com.bryan.forge.ledger.backend.dto.RecordMovementRequest;
import com.bryan.forge.ledger.datamodel.Account;
import com.bryan.forge.ledger.datamodel.AccountKind;
import com.bryan.forge.ledger.datamodel.Movement;
import com.bryan.forge.ledger.datamodel.MovementType;
import com.bryan.forge.ledger.datarepository.AccountRepository;
import com.bryan.forge.ledger.datarepository.MovementRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LedgerServiceTest {

    private final AccountRepository accountRepo = mock(AccountRepository.class);
    private final MovementRepository movementRepo = mock(MovementRepository.class);
    private final ItemRepository itemRepo = mock(ItemRepository.class);
    private final BusinessRepository businessRepo = mock(BusinessRepository.class);
    private final BusinessAccessService access = mock(BusinessAccessService.class);
    private final EntityManager em = mock(EntityManager.class);
    @SuppressWarnings("unchecked")
    private final io.micronaut.context.event.ApplicationEventPublisher<Object> events =
            mock(io.micronaut.context.event.ApplicationEventPublisher.class);
    private final LedgerService service = new LedgerService(accountRepo, movementRepo, itemRepo, businessRepo, access, em, events);

    private final User actor = mock(User.class);
    private final UUID biz = UUID.randomUUID();
    private final UUID acc = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    private Movement movement(int qty, UUID from, UUID to) {
        return new Movement(biz, itemId, qty, from, to, MovementType.TRANSFER, "MANUAL", null, null, null);
    }

    @Test
    void projectionSoldeCorrect() {
        Business business = mock(Business.class);
        Account account = new Account(biz, "Coffre", AccountKind.STOCK);
        Item item = mock(Item.class);
        when(item.getId()).thenReturn(itemId);
        when(item.getName()).thenReturn("Lingot");
        // Série : +10 (entrée), -3 (sortie) → solde attendu 7.
        Movement in = movement(10, null, acc);
        Movement out = movement(3, acc, null);

        when(businessRepo.findById(biz)).thenReturn(Optional.of(business));
        when(accountRepo.findById(acc)).thenReturn(Optional.of(account));
        when(movementRepo.findByFromAccountIdOrToAccountId(acc, acc)).thenReturn(List.of(in, out));
        when(itemRepo.findAll()).thenReturn(List.of(item));

        List<ItemBalanceDto> balances = service.balances(actor, biz, acc);

        assertThat(balances).hasSize(1);
        assertThat(balances.get(0).balance()).isEqualTo(7L);
        assertThat(balances.get(0).itemName()).isEqualTo("Lingot");
    }

    @Test
    void refuseStockNegatif() {
        Business business = mock(Business.class);
        Account from = new Account(biz, "Stock", AccountKind.STOCK);
        Item item = mock(Item.class);
        when(item.getName()).thenReturn("Épée");
        Movement in = movement(2, null, acc); // disponible : 2

        when(businessRepo.findById(biz)).thenReturn(Optional.of(business));
        when(accountRepo.findById(acc)).thenReturn(Optional.of(from));
        when(itemRepo.findById(itemId)).thenReturn(Optional.of(item));
        when(movementRepo.findByFromAccountIdOrToAccountId(acc, acc)).thenReturn(List.of(in));

        var req = new RecordMovementRequest(itemId, 5, acc, null, MovementType.WITHDRAWAL, null);

        assertThatThrownBy(() -> service.record(actor, biz, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("insuffisant");
        verify(movementRepo, never()).save(any());
    }

    @Test
    void enregistreMouvementValide() {
        Business business = mock(Business.class);
        Account from = new Account(biz, "Stock", AccountKind.STOCK);
        Item item = mock(Item.class);
        when(item.getName()).thenReturn("Épée");
        Movement in = movement(10, null, acc); // disponible : 10

        when(businessRepo.findById(biz)).thenReturn(Optional.of(business));
        when(accountRepo.findById(acc)).thenReturn(Optional.of(from));
        when(itemRepo.findById(itemId)).thenReturn(Optional.of(item));
        when(movementRepo.findByFromAccountIdOrToAccountId(acc, acc)).thenReturn(List.of(in));
        when(itemRepo.findAll()).thenReturn(List.of(item));
        when(accountRepo.findByBusinessId(biz)).thenReturn(List.of());
        when(movementRepo.save(any(Movement.class))).thenAnswer(inv -> inv.getArgument(0));

        var req = new RecordMovementRequest(itemId, 4, acc, null, MovementType.WITHDRAWAL, "vente");
        var dto = service.record(actor, biz, req);

        assertThat(dto.quantity()).isEqualTo(4);
        verify(movementRepo).save(any(Movement.class));
    }

    @Test
    void recordBatch_enregistreChaqueDepot() {
        when(businessRepo.findById(biz)).thenReturn(Optional.of(mock(Business.class)));
        when(accountRepo.findById(acc)).thenReturn(Optional.of(new Account(biz, "Coffre", AccountKind.STOCK)));
        when(itemRepo.findById(any())).thenReturn(Optional.of(mock(Item.class)));
        when(movementRepo.save(any(Movement.class))).thenAnswer(inv -> inv.getArgument(0));

        int n = service.recordBatch(actor, biz, List.of(
                new RecordMovementRequest(itemId, 100, null, acc, MovementType.DEPOSIT, null),
                new RecordMovementRequest(UUID.randomUUID(), 50, null, acc, MovementType.DEPOSIT, null)));

        assertThat(n).isEqualTo(2);
        verify(movementRepo, times(2)).save(any(Movement.class));
    }

    @Test
    void recordBatch_refuseQuantiteInvalide() {
        when(businessRepo.findById(biz)).thenReturn(Optional.of(mock(Business.class)));

        assertThatThrownBy(() -> service.recordBatch(actor, biz, List.of(
                new RecordMovementRequest(itemId, 0, null, acc, MovementType.DEPOSIT, null))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
