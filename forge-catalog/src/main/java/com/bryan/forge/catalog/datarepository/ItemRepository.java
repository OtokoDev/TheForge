package com.bryan.forge.catalog.datarepository;

import com.bryan.forge.catalog.datamodel.Item;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {

    /** L'item système (le septime). */
    Optional<Item> findFirstBySystemTrue();
}
