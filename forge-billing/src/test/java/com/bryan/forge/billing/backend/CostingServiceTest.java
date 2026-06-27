package com.bryan.forge.billing.backend;

import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datamodel.RecipeComponent;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.catalog.datarepository.RecipeComponentRepository;
import com.bryan.forge.valuation.datamodel.Product;
import com.bryan.forge.valuation.datarepository.ProductRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CostingServiceTest {

    private final RecipeComponentRepository recipeRepo = mock(RecipeComponentRepository.class);
    private final ItemRepository itemRepo = mock(ItemRepository.class);
    private final ProductRepository productRepo = mock(ProductRepository.class);
    private final BusinessRepository businessRepo = mock(BusinessRepository.class);
    private final BusinessAccessService access = mock(BusinessAccessService.class);
    private final CostingService service = new CostingService(recipeRepo, itemRepo, productRepo, businessRepo, access);

    private final UUID biz = UUID.randomUUID();

    private RecipeComponent component(UUID componentItemId, int qty) {
        Item item = mock(Item.class);
        when(item.getId()).thenReturn(componentItemId);
        RecipeComponent rc = mock(RecipeComponent.class);
        when(rc.getComponentItem()).thenReturn(item);
        when(rc.getQuantity()).thenReturn(qty);
        return rc;
    }

    @Test
    void coutRecursif() {
        // sword = 2× ingot ; ingot = 3× ore ; ore (brut) valeur 5 → ingot=15, sword=30.
        UUID sword = UUID.randomUUID();
        UUID ingot = UUID.randomUUID();
        UUID ore = UUID.randomUUID();
        Item oreItem = mock(Item.class);
        when(oreItem.isSystem()).thenReturn(false);
        RecipeComponent swordToIngot = component(ingot, 2);
        RecipeComponent ingotToOre = component(ore, 3);

        when(recipeRepo.findByOutputItemId(sword)).thenReturn(List.of(swordToIngot));
        when(recipeRepo.findByOutputItemId(ingot)).thenReturn(List.of(ingotToOre));
        when(recipeRepo.findByOutputItemId(ore)).thenReturn(List.of());
        when(itemRepo.findById(ore)).thenReturn(Optional.of(oreItem));
        when(productRepo.findByBusinessIdAndItemIdAndValidToIsNull(biz, ore))
                .thenReturn(Optional.of(new Product(biz, ore, new BigDecimal("5"), null)));

        assertThat(service.costOf(biz, sword)).isEqualByComparingTo("30");
        assertThat(service.costOf(biz, ingot)).isEqualByComparingTo("15");
    }

    @Test
    void itemSimpleSansValeurCouteZero() {
        UUID raw = UUID.randomUUID();
        Item rawItem = mock(Item.class);
        when(rawItem.isSystem()).thenReturn(false);
        when(recipeRepo.findByOutputItemId(raw)).thenReturn(List.of());
        when(itemRepo.findById(raw)).thenReturn(Optional.of(rawItem));
        when(productRepo.findByBusinessIdAndItemIdAndValidToIsNull(biz, raw)).thenReturn(Optional.empty());

        assertThat(service.costOf(biz, raw)).isEqualByComparingTo("0");
    }
}
