package com.bryan.forge.treasury.backend;

import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.core.backend.AuditService;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.core.datarepository.UserRepository;
import com.bryan.forge.ledger.backend.LedgerService;
import com.bryan.forge.ledger.datamodel.Account;
import com.bryan.forge.ledger.datamodel.AccountKind;
import com.bryan.forge.ledger.datarepository.AccountRepository;
import com.bryan.forge.treasury.backend.dto.CreanceFarmerDto;
import com.bryan.forge.treasury.backend.dto.DepositLine;
import com.bryan.forge.treasury.datamodel.CreanceEntry;
import com.bryan.forge.treasury.datamodel.CreanceType;
import com.bryan.forge.treasury.datarepository.CreanceEntryRepository;
import com.bryan.forge.valuation.datamodel.Product;
import com.bryan.forge.valuation.datarepository.ProductRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreanceServiceTest {

    private final CreanceEntryRepository repo = mock(CreanceEntryRepository.class);
    private final ItemRepository itemRepo = mock(ItemRepository.class);
    private final ProductRepository productRepo = mock(ProductRepository.class);
    private final LedgerService ledgerService = mock(LedgerService.class);
    private final AccountRepository accountRepo = mock(AccountRepository.class);
    private final BusinessRepository businessRepo = mock(BusinessRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final BusinessAccessService access = mock(BusinessAccessService.class);
    private final AuditService audit = mock(AuditService.class);
    private final CreanceService service = new CreanceService(repo, itemRepo, productRepo, ledgerService,
            accountRepo, businessRepo, userRepo, access, audit);

    private final UUID biz = UUID.randomUUID();
    private final String farmer = "Bob le farmeur"; // nom libre
    private final UUID ore = UUID.randomUUID();
    private final UUID account = UUID.randomUUID();
    private final User actor = mock(User.class);

    @Test
    void depotAuCoutValoriseArrondiParExcesEtCreeCredit() {
        Item oreItem = mock(Item.class);
        when(oreItem.isSystem()).thenReturn(false);
        when(businessRepo.findById(biz)).thenReturn(Optional.of(mock(Business.class)));
        when(accountRepo.findById(account)).thenReturn(Optional.of(new Account(biz, "Stock", AccountKind.STOCK)));
        when(itemRepo.findById(ore)).thenReturn(Optional.of(oreItem));
        // 3 × 0,5 = 1,5 → arrondi par excès = 2
        when(productRepo.findByBusinessIdAndItemIdAndValidToIsNull(biz, ore))
                .thenReturn(Optional.of(new Product(biz, ore, new BigDecimal("0.5"), null)));
        when(repo.findByBusinessIdAndFarmerNameOrderByCreatedAtDesc(biz, farmer)).thenReturn(List.of());

        service.deposit(actor, biz, farmer, List.of(new DepositLine(ore, 3, null)), account, "minerai");

        verify(ledgerService, times(1)).applyMovement(any(), any(), anyInt(), any(), any(), any(), any(), any(), any(), any());
        verify(repo).save(argThat(e -> e.getType() == CreanceType.CREDIT && e.getAmount() == 2L
                && e.getFarmerName().equals(farmer)));
    }

    @Test
    void depotAvecPrixNegocieUtiliseLePrixFourni() {
        Item oreItem = mock(Item.class);
        when(businessRepo.findById(biz)).thenReturn(Optional.of(mock(Business.class)));
        when(accountRepo.findById(account)).thenReturn(Optional.of(new Account(biz, "Stock", AccountKind.STOCK)));
        when(itemRepo.findById(ore)).thenReturn(Optional.of(oreItem));
        when(repo.findByBusinessIdAndFarmerNameOrderByCreatedAtDesc(biz, farmer)).thenReturn(List.of());

        // 3 × 10 = 30 (prix d'achat négocié ; le coût catalogue n'est pas consulté)
        service.deposit(actor, biz, farmer, List.of(new DepositLine(ore, 3, new BigDecimal("10"))), account, null);

        verify(repo).save(argThat(e -> e.getType() == CreanceType.CREDIT && e.getAmount() == 30L));
    }

    @Test
    void paiementSortDuCoffreEtCalculeResteDu() {
        Item septime = mock(Item.class);
        when(septime.getId()).thenReturn(UUID.randomUUID());
        when(businessRepo.findById(biz)).thenReturn(Optional.of(mock(Business.class)));
        when(accountRepo.findById(account)).thenReturn(Optional.of(new Account(biz, "Coffre", AccountKind.COFFRE)));
        when(itemRepo.findFirstBySystemTrue()).thenReturn(Optional.of(septime));
        // Historique après paiement : crédité 100, payé 30 → reste dû 70.
        UUID author = UUID.randomUUID();
        List<CreanceEntry> history = List.of(
                new CreanceEntry(biz, farmer, CreanceType.CREDIT, 100, null, author),
                new CreanceEntry(biz, farmer, CreanceType.PAIEMENT, 30, null, author));
        when(repo.findByBusinessIdAndFarmerNameOrderByCreatedAtDesc(biz, farmer)).thenReturn(history);

        CreanceFarmerDto dto = service.pay(actor, biz, farmer, 30, account, "acompte");

        verify(ledgerService, times(1)).applyMovement(any(), any(), anyInt(), any(), any(), any(), any(), any(), any(), any());
        verify(repo).save(argThat(e -> e.getType() == CreanceType.PAIEMENT && e.getAmount() == 30L));
        assertThat(dto.farmerName()).isEqualTo(farmer);
        assertThat(dto.totalCredit()).isEqualTo(100L);
        assertThat(dto.totalPaid()).isEqualTo(30L);
        assertThat(dto.remaining()).isEqualTo(70L);
    }
}
