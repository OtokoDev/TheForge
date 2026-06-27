package com.bryan.forge.valuation.backend;

import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.catalog.datarepository.RecipeComponentRepository;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.valuation.backend.dto.ProductDto;
import com.bryan.forge.valuation.datamodel.Product;
import com.bryan.forge.valuation.datarepository.ProductRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private final ProductRepository repo = mock(ProductRepository.class);
    private final ItemRepository itemRepo = mock(ItemRepository.class);
    private final RecipeComponentRepository recipeRepo = mock(RecipeComponentRepository.class);
    private final BusinessRepository businessRepo = mock(BusinessRepository.class);
    private final BusinessAccessService access = mock(BusinessAccessService.class);
    private final ProductService service = new ProductService(repo, itemRepo, recipeRepo, businessRepo, access);

    private final User actor = mock(User.class);
    private final UUID biz = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    private Item item(boolean system) {
        Item it = mock(Item.class);
        when(it.getId()).thenReturn(itemId);
        when(it.getName()).thenReturn("Item");
        when(it.isSystem()).thenReturn(system);
        return it;
    }

    @Test
    void matiere_prixReventeDefautEgalValeur() {
        Business business = mock(Business.class);
        Item it = item(false);
        when(businessRepo.findById(biz)).thenReturn(Optional.of(business));
        when(itemRepo.findById(itemId)).thenReturn(Optional.of(it));
        when(recipeRepo.existsByOutputItemId(itemId)).thenReturn(false);
        when(repo.findByBusinessIdAndItemIdAndValidToIsNull(biz, itemId)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductDto dto = service.setProduct(actor, biz, itemId, new BigDecimal("10"), null, 0);

        assertThat(dto.valeur()).isEqualByComparingTo("10");
        assertThat(dto.prixRevente()).isEqualByComparingTo("10"); // défaut = valeur
        assertThat(dto.hasRecipe()).isFalse();
    }

    @Test
    void craftable_valeurForceeNull_prixReventeIndependant() {
        Business business = mock(Business.class);
        Item it = item(false);
        when(businessRepo.findById(biz)).thenReturn(Optional.of(business));
        when(itemRepo.findById(itemId)).thenReturn(Optional.of(it));
        when(recipeRepo.existsByOutputItemId(itemId)).thenReturn(true);
        when(repo.findByBusinessIdAndItemIdAndValidToIsNull(biz, itemId)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductDto dto = service.setProduct(actor, biz, itemId, new BigDecimal("99"), new BigDecimal("50"), 0);

        assertThat(dto.valeur()).isNull();          // craftable → valeur dérivée, non stockée
        assertThat(dto.prixRevente()).isEqualByComparingTo("50");
        assertThat(dto.hasRecipe()).isTrue();
    }
}
