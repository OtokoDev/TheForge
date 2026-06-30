package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.CreateProductionRequest;
import com.bryan.forge.billing.backend.dto.ProductionOrderDto;
import com.bryan.forge.billing.datamodel.ProductionOrder;
import com.bryan.forge.billing.datamodel.ProductionStatus;
import com.bryan.forge.billing.datarepository.ProductionOrderRepository;
import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datamodel.RecipeComponent;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.catalog.datarepository.RecipeComponentRepository;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.ledger.backend.LedgerService;
import com.bryan.forge.ledger.datamodel.MovementType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductionServiceTest {

    private final ProductionOrderRepository repo = mock(ProductionOrderRepository.class);
    private final RecipeComponentRepository recipeRepo = mock(RecipeComponentRepository.class);
    private final ItemRepository itemRepo = mock(ItemRepository.class);
    private final BusinessRepository businessRepo = mock(BusinessRepository.class);
    private final BusinessAccessService access = mock(BusinessAccessService.class);
    private final LedgerService ledgerService = mock(LedgerService.class);
    private final ProductionService service = new ProductionService(repo, recipeRepo, itemRepo,
            businessRepo, access, ledgerService);

    private final User actor = mock(User.class);
    private final UUID biz = UUID.randomUUID();
    private final UUID oid = UUID.randomUUID();
    private final UUID itemX = UUID.randomUUID();
    private final UUID stock = UUID.randomUUID();

    @Test
    void create_refuseSansRecette() {
        when(businessRepo.findById(biz)).thenReturn(Optional.of(mock(Business.class)));
        when(actor.getId()).thenReturn(UUID.randomUUID());
        when(itemRepo.findById(itemX)).thenReturn(Optional.of(mock(Item.class)));
        when(recipeRepo.findByOutputItemId(itemX)).thenReturn(List.of()); // pas de recette

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> service.create(actor, biz, new CreateProductionRequest(itemX, 3, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void complete_consommeIngredientsEtProduitLObjet() {
        ProductionOrder o = mock(ProductionOrder.class);
        when(o.getBusinessId()).thenReturn(biz);
        when(o.getStatus()).thenReturn(ProductionStatus.EN_COURS);
        when(o.getOutputItemId()).thenReturn(itemX);
        when(o.getQuantity()).thenReturn(3);
        when(o.getId()).thenReturn(oid);
        when(o.getNumero()).thenReturn(7L);
        when(repo.findById(oid)).thenReturn(Optional.of(o));
        Business b = mock(Business.class);
        when(b.getDefaultStockAccountId()).thenReturn(stock);
        when(businessRepo.findById(biz)).thenReturn(Optional.of(b));
        when(actor.getId()).thenReturn(UUID.randomUUID());
        UUID ironId = UUID.randomUUID();
        Item iron = mock(Item.class);
        when(iron.getId()).thenReturn(ironId);
        RecipeComponent rc = mock(RecipeComponent.class);
        when(rc.getComponentItem()).thenReturn(iron);
        when(rc.getQuantity()).thenReturn(2);
        when(recipeRepo.findByOutputItemId(itemX)).thenReturn(List.of(rc));
        when(itemRepo.findAll()).thenReturn(List.of());

        ProductionOrderDto dto = service.complete(actor, biz, oid);

        // 2 fer × 3 objets = 6 consommés du stock
        verify(ledgerService).applyMovement(eq(biz), eq(ironId), eq(6), eq(stock), isNull(),
                eq(MovementType.CONSUMPTION), eq("PRODUCTION"), eq(oid), any(), any());
        // 3 objets produits vers le stock
        verify(ledgerService).applyMovement(eq(biz), eq(itemX), eq(3), isNull(), eq(stock),
                eq(MovementType.PRODUCTION), eq("PRODUCTION"), eq(oid), any(), any());
        verify(o).setStatus(ProductionStatus.TERMINEE);
        assertThat(dto.outputItemId()).isEqualTo(itemX);
    }
}
