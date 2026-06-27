package com.bryan.forge.valuation.backend;

import com.bryan.forge.business.backend.CurrentUser;
import com.bryan.forge.valuation.backend.dto.ProductDto;
import com.bryan.forge.valuation.backend.dto.ProductHistoryDto;
import com.bryan.forge.valuation.backend.dto.SetProductRequest;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Put;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.util.List;
import java.util.UUID;

/**
 * Produits PAR BUSINESS (historisés). Lecture pour les membres du business (et SYSTEM/STAFF) ;
 * écriture pour un ADMIN du business (ou SYSTEM). Autorisation fine portée par le service.
 */
@Controller("/api/businesses/{businessId}/products")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_AUTHENTICATED)
public class ProductController {

    private final ProductService productService;
    private final CurrentUser currentUser;

    public ProductController(ProductService productService, CurrentUser currentUser) {
        this.productService = productService;
        this.currentUser = currentUser;
    }

    @Get
    public List<ProductDto> listCurrent(UUID businessId) {
        return productService.listCurrent(currentUser.require(), businessId);
    }

    @Put("/{itemId}")
    public ProductDto setProduct(UUID businessId, UUID itemId, @Body SetProductRequest req) {
        return productService.setProduct(currentUser.require(), businessId, itemId, req.valeur(), req.prixRevente(), req.version());
    }

    @Get("/{itemId}/history")
    public List<ProductHistoryDto> history(UUID businessId, UUID itemId) {
        return productService.history(currentUser.require(), businessId, itemId);
    }
}
