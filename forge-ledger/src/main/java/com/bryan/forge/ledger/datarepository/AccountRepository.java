package com.bryan.forge.ledger.datarepository;

import com.bryan.forge.ledger.datamodel.Account;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByBusinessId(UUID businessId);
}
