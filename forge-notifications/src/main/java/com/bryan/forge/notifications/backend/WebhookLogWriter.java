package com.bryan.forge.notifications.backend;

import com.bryan.forge.notifications.datamodel.WebhookLog;
import com.bryan.forge.notifications.datarepository.WebhookLogRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

/**
 * Écrit le journal des webhooks dans sa propre transaction courte. Bean séparé
 * (et non une méthode de {@link WebhookService}) car l'appel doit traverser le
 * proxy pour que {@code @Transactional} s'applique : les listeners {@code @Async}
 * tournent hors de toute session/transaction (cf. {@code Session is closed}).
 */
@Singleton
public class WebhookLogWriter {

    private final WebhookLogRepository repo;

    public WebhookLogWriter(WebhookLogRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void record(String type, String payload, boolean success, String error) {
        repo.save(new WebhookLog(type, payload, success, error));
    }
}
