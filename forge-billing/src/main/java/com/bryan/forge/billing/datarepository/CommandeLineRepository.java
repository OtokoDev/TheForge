package com.bryan.forge.billing.datarepository;

import com.bryan.forge.billing.datamodel.CommandeLine;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommandeLineRepository extends JpaRepository<CommandeLine, UUID> {

    List<CommandeLine> findByCommandeId(UUID commandeId);

    void deleteByCommandeId(UUID commandeId);
}
