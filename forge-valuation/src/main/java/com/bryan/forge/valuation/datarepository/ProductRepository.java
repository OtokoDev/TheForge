package com.bryan.forge.valuation.datarepository;

import com.bryan.forge.valuation.datamodel.Product;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByBusinessIdAndItemIdAndValidToIsNull(UUID businessId, UUID itemId);

    List<Product> findByBusinessIdAndValidToIsNull(UUID businessId);

    List<Product> findByBusinessIdAndItemIdOrderByValidFromDesc(UUID businessId, UUID itemId);
}
