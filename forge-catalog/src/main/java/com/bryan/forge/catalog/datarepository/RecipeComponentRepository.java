package com.bryan.forge.catalog.datarepository;

import com.bryan.forge.catalog.datamodel.RecipeComponent;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecipeComponentRepository extends JpaRepository<RecipeComponent, UUID> {

    List<RecipeComponent> findByOutputItemId(UUID outputItemId);

    void deleteByOutputItemId(UUID outputItemId);

    boolean existsByComponentItemId(UUID componentItemId);

    boolean existsByOutputItemId(UUID outputItemId);
}
