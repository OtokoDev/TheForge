package com.bryan.forge.notifications.datarepository;

import com.bryan.forge.notifications.datamodel.WebhookLog;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.UUID;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, UUID> {
}
