package com.bryan.forge.business.datarepository;

import com.bryan.forge.business.datamodel.Membership;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    List<Membership> findByUserId(UUID userId);

    List<Membership> findByBusinessId(UUID businessId);

    Optional<Membership> findByUserIdAndBusinessId(UUID userId, UUID businessId);
}
