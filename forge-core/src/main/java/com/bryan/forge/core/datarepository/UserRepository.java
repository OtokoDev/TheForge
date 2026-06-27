package com.bryan.forge.core.datarepository;

import com.bryan.forge.core.datamodel.User;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByDiscordId(String discordId);
}
