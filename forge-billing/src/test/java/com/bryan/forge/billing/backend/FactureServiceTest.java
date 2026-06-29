package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.CreateFactureLine;
import com.bryan.forge.billing.backend.dto.CreateFactureRequest;
import com.bryan.forge.billing.backend.dto.FactureDto;
import com.bryan.forge.billing.datamodel.Facture;
import com.bryan.forge.billing.datamodel.FactureLine;
import com.bryan.forge.billing.datamodel.FactureStatus;
import com.bryan.forge.billing.datamodel.TaxBase;
import com.bryan.forge.billing.datarepository.FactureLineRepository;
import com.bryan.forge.billing.datarepository.FactureRepository;
import com.bryan.forge.billing.datarepository.SessionRepository;
import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datamodel.RecipeComponent;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.catalog.datarepository.RecipeComponentRepository;
import com.bryan.forge.ledger.backend.LedgerService;
import com.bryan.forge.ledger.datamodel.Account;
import com.bryan.forge.ledger.datamodel.AccountKind;
import com.bryan.forge.ledger.datamodel.MovementType;
import com.bryan.forge.ledger.datarepository.AccountRepository;
import com.bryan.forge.valuation.datarepository.ProductRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FactureServiceTest {

    private final FactureRepository factureRepo = mock(FactureRepository.class);
    private final FactureLineRepository lineRepo = mock(FactureLineRepository.class);
    private final SessionRepository sessionRepo = mock(SessionRepository.class);
    private final ItemRepository itemRepo = mock(ItemRepository.class);
    private final RecipeComponentRepository recipeRepo = mock(RecipeComponentRepository.class);
    private final ProductRepository productRepo = mock(ProductRepository.class);
    private final CostingService costingService = mock(CostingService.class);
    private final TaxRateService taxRateService = mock(TaxRateService.class);
    private final LedgerService ledgerService = mock(LedgerService.class);
    private final AccountRepository accountRepo = mock(AccountRepository.class);
    private final BusinessRepository businessRepo = mock(BusinessRepository.class);
    private final BusinessAccessService access = mock(BusinessAccessService.class);
    @SuppressWarnings("unchecked")
    private final io.micronaut.context.event.ApplicationEventPublisher<Object> events =
            mock(io.micronaut.context.event.ApplicationEventPublisher.class);
    private final FactureService service = new FactureService(factureRepo, lineRepo, sessionRepo, itemRepo,
            recipeRepo, productRepo, costingService, taxRateService, ledgerService, accountRepo, businessRepo, access, events);

    private final com.bryan.forge.core.datamodel.User actor = mock(com.bryan.forge.core.datamodel.User.class);
    private final UUID biz = UUID.randomUUID();
    private final UUID fid = UUID.randomUUID();
    private final UUID itemX = UUID.randomUUID();
    private final UUID stock = UUID.randomUUID();
    private final UUID coffre = UUID.randomUUID();

    @Test
    void validation_arrondiParExces_split_etMouvements() {
        Business business = mock(Business.class);
        Facture facture = new Facture(biz, 1, UUID.randomUUID(), null, null);
        // 3 × 0,1 = 0,3 → arrondi par excès = 1
        FactureLine line = new FactureLine(fid, itemX, 3, new BigDecimal("0.1"));
        Item septime = mock(Item.class);
        when(septime.getId()).thenReturn(UUID.randomUUID());

        when(businessRepo.findById(biz)).thenReturn(Optional.of(business));
        when(factureRepo.findById(fid)).thenReturn(Optional.of(facture));
        when(accountRepo.findById(stock)).thenReturn(Optional.of(new Account(biz, "Stock", AccountKind.STOCK)));
        when(accountRepo.findById(coffre)).thenReturn(Optional.of(new Account(biz, "Coffre", AccountKind.COFFRE)));
        when(lineRepo.findByFactureId(fid)).thenReturn(List.of(line));
        when(costingService.costOf(biz, itemX)).thenReturn(BigDecimal.ZERO);
        when(taxRateService.currentRate(biz)).thenReturn(new BigDecimal("0.5"));
        when(itemRepo.findFirstBySystemTrue()).thenReturn(Optional.of(septime));
        when(itemRepo.findAll()).thenReturn(List.of());
        when(ledgerService.balanceOf(stock, itemX)).thenReturn(3L); // en stock → vendu depuis le stock

        FactureDto dto = service.validate(actor, biz, fid, true, stock, coffre);

        assertThat(dto.status()).isEqualTo(FactureStatus.VALIDEE);
        assertThat(dto.totalAmount()).isEqualTo(1L);            // arrondi par excès
        assertThat(dto.totalCost()).isEqualByComparingTo("0");
        assertThat(dto.totalProfit()).isEqualByComparingTo("1");
        assertThat(dto.businessShare()).isEqualByComparingTo("0.5");
        assertThat(dto.workerShare()).isEqualByComparingTo("0.5");
        // 1 mouvement marchandise + 1 mouvement septimes.
        verify(ledgerService, times(2)).applyMovement(any(), any(), anyInt(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void forgeALaVente_pasEnStock_consommeLesIngredients() {
        Business business = mock(Business.class);
        Facture facture = new Facture(biz, 1, UUID.randomUUID(), null, null);
        FactureLine line = new FactureLine(fid, itemX, 2, new BigDecimal("10")); // 2 objets vendus
        Item septime = mock(Item.class);
        when(septime.getId()).thenReturn(UUID.randomUUID());
        UUID ferId = UUID.randomUUID();
        Item fer = mock(Item.class);
        when(fer.getId()).thenReturn(ferId);
        RecipeComponent rc = mock(RecipeComponent.class);
        when(rc.getComponentItem()).thenReturn(fer);
        when(rc.getQuantity()).thenReturn(3); // 3 fer par objet

        when(businessRepo.findById(biz)).thenReturn(Optional.of(business));
        when(factureRepo.findById(fid)).thenReturn(Optional.of(facture));
        when(accountRepo.findById(stock)).thenReturn(Optional.of(new Account(biz, "Stock", AccountKind.STOCK)));
        when(accountRepo.findById(coffre)).thenReturn(Optional.of(new Account(biz, "Coffre", AccountKind.COFFRE)));
        when(lineRepo.findByFactureId(fid)).thenReturn(List.of(line));
        when(costingService.costOf(biz, itemX)).thenReturn(BigDecimal.ZERO);
        when(taxRateService.currentRate(biz)).thenReturn(new BigDecimal("0.5"));
        when(itemRepo.findFirstBySystemTrue()).thenReturn(Optional.of(septime));
        when(itemRepo.findAll()).thenReturn(List.of());
        when(ledgerService.balanceOf(stock, itemX)).thenReturn(0L);      // pas en stock → forge
        when(recipeRepo.findByOutputItemId(itemX)).thenReturn(List.of(rc));

        service.validate(actor, biz, fid, true, stock, coffre);

        // Ingrédient consommé depuis le stock : 3 fer × 2 objets = 6
        verify(ledgerService).applyMovement(eq(biz), eq(ferId), eq(6), eq(stock), isNull(),
                eq(MovementType.CONSUMPTION), eq("FACTURE"), eq(fid), any(), any());
        // L'objet fini n'est jamais sorti du stock
        verify(ledgerService, never()).applyMovement(any(), eq(itemX), anyInt(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void deleteDraft_createur_supprime() {
        UUID creator = UUID.randomUUID();
        when(actor.getId()).thenReturn(creator);
        Facture facture = new Facture(biz, 1, creator, null, null); // BROUILLON par défaut
        when(businessRepo.findById(biz)).thenReturn(Optional.of(mock(Business.class)));
        when(factureRepo.findById(fid)).thenReturn(Optional.of(facture));

        service.deleteDraft(actor, biz, fid);

        verify(lineRepo).deleteByFactureId(fid);
        verify(factureRepo).delete(facture);
        verify(access, never()).requireAdmin(actor, biz); // créateur → pas de contrôle admin
    }

    @Test
    void deleteDraft_nonCreateur_exigeAdmin() {
        when(actor.getId()).thenReturn(UUID.randomUUID());
        Facture facture = new Facture(biz, 1, UUID.randomUUID(), null, null); // créé par un autre
        when(businessRepo.findById(biz)).thenReturn(Optional.of(mock(Business.class)));
        when(factureRepo.findById(fid)).thenReturn(Optional.of(facture));

        service.deleteDraft(actor, biz, fid);

        verify(access).requireAdmin(actor, biz); // non-créateur → contrôle admin
    }

    @Test
    void replaceDraft_remplaceLesLignes() {
        UUID creator = UUID.randomUUID();
        when(actor.getId()).thenReturn(creator);
        Facture facture = new Facture(biz, 1, creator, null, null);
        when(businessRepo.findById(biz)).thenReturn(Optional.of(mock(Business.class)));
        when(factureRepo.findById(fid)).thenReturn(Optional.of(facture));
        when(itemRepo.findById(itemX)).thenReturn(Optional.of(mock(Item.class)));
        when(productRepo.findByBusinessIdAndItemIdAndValidToIsNull(biz, itemX)).thenReturn(Optional.empty());
        when(lineRepo.findByFactureId(fid)).thenReturn(List.of());
        when(itemRepo.findAll()).thenReturn(List.of());

        service.replaceDraft(actor, biz, fid,
                new CreateFactureRequest(List.of(new CreateFactureLine(itemX, 2, null)), "Client", null));

        verify(lineRepo).deleteByFactureId(fid);   // anciennes lignes effacées
        verify(lineRepo).save(any(FactureLine.class)); // nouvelle ligne insérée
    }

    @Test
    void taxeSurLeCA_partBusinessSurLeChiffreDAffaires() {
        Business business = mock(Business.class);
        Facture facture = new Facture(biz, 1, UUID.randomUUID(), null, null);
        FactureLine line = new FactureLine(fid, itemX, 1, new BigDecimal("100")); // CA = 100
        Item septime = mock(Item.class);
        when(septime.getId()).thenReturn(UUID.randomUUID());

        when(businessRepo.findById(biz)).thenReturn(Optional.of(business));
        when(factureRepo.findById(fid)).thenReturn(Optional.of(facture));
        when(accountRepo.findById(stock)).thenReturn(Optional.of(new Account(biz, "Stock", AccountKind.STOCK)));
        when(accountRepo.findById(coffre)).thenReturn(Optional.of(new Account(biz, "Coffre", AccountKind.COFFRE)));
        when(lineRepo.findByFactureId(fid)).thenReturn(List.of(line));
        when(costingService.costOf(biz, itemX)).thenReturn(new BigDecimal("60")); // bénéfice = 40
        when(taxRateService.currentRate(biz)).thenReturn(new BigDecimal("0.1"));
        when(taxRateService.currentBase(biz)).thenReturn(TaxBase.REVENUE);
        when(itemRepo.findFirstBySystemTrue()).thenReturn(Optional.of(septime));
        when(itemRepo.findAll()).thenReturn(List.of());
        when(ledgerService.balanceOf(stock, itemX)).thenReturn(5L); // en stock

        FactureDto dto = service.validate(actor, biz, fid, true, stock, coffre);

        assertThat(dto.totalAmount()).isEqualTo(100L);
        assertThat(dto.totalProfit()).isEqualByComparingTo("40");
        assertThat(dto.businessShare()).isEqualByComparingTo("10"); // 100 × 0,1 sur le CA
        assertThat(dto.workerShare()).isEqualByComparingTo("30");   // bénéfice 40 − 10
    }

    @Test
    void refuseRevalidation() {
        Business business = mock(Business.class);
        Facture facture = new Facture(biz, 1, UUID.randomUUID(), null, null);
        facture.setStatus(FactureStatus.VALIDEE);

        when(businessRepo.findById(biz)).thenReturn(Optional.of(business));
        when(factureRepo.findById(fid)).thenReturn(Optional.of(facture));
        when(accountRepo.findById(stock)).thenReturn(Optional.of(new Account(biz, "Stock", AccountKind.STOCK)));
        when(accountRepo.findById(coffre)).thenReturn(Optional.of(new Account(biz, "Coffre", AccountKind.COFFRE)));

        assertThatThrownBy(() -> service.validate(actor, biz, fid, true, stock, coffre))
                .isInstanceOf(IllegalStateException.class);
    }
}
