package com.bryan.forge.catalog.datarepository;

import com.bryan.forge.catalog.datamodel.Taxon;
import com.bryan.forge.catalog.datamodel.TaxonKind;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaxonRepository extends JpaRepository<Taxon, UUID> {

    List<Taxon> findByKind(TaxonKind kind);

    boolean existsByIdAndKind(UUID id, TaxonKind kind);
}
