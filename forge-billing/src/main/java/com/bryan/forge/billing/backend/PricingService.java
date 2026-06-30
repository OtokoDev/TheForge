package com.bryan.forge.billing.backend;

import com.bryan.forge.valuation.datamodel.Product;
import com.bryan.forge.valuation.datarepository.ProductRepository;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Résolution du prix unitaire d'une ligne (facture / commande). */
@Singleton
public class PricingService {

    private final ProductRepository productRepo;

    public PricingService(ProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    /** Prix négocié si fourni (≥ 0), sinon prix de revente catalogue courant, sinon 0. */
    public BigDecimal resolveUnitPrice(UUID businessId, UUID itemId, BigDecimal negotiated) {
        if (negotiated != null && negotiated.signum() >= 0) {
            return negotiated;
        }
        return productRepo.findByBusinessIdAndItemIdAndValidToIsNull(businessId, itemId)
                .map(Product::getPrixRevente).filter(Objects::nonNull).orElse(BigDecimal.ZERO);
    }
}
