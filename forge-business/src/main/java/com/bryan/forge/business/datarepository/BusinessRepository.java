package com.bryan.forge.business.datarepository;

import com.bryan.forge.business.datamodel.Business;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.UUID;

@Repository
public interface BusinessRepository extends JpaRepository<Business, UUID> {
}
