package com.bryan.forge.notifications.backend;

import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Envoie un payload Discord (embed) avec retry (3 tentatives) et journalise chaque envoi. */
@Singleton
public class WebhookService {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookService.class);

    private final ObjectMapper objectMapper;
    private final WebhookLogWriter logWriter;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public WebhookService(ObjectMapper objectMapper, WebhookLogWriter logWriter) {
        this.objectMapper = objectMapper;
        this.logWriter = logWriter;
    }

    public void send(String url, String type, Map<String, Object> payload) {
        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (IOException e) {
            logWriter.record(type, "<sérialisation impossible>", false, e.getMessage());
            return;
        }

        boolean success = false;
        String error = null;
        if (url == null || url.isBlank()) {
            error = "Webhook non configuré";
        } else {
            for (int attempt = 1; attempt <= 3 && !success; attempt++) {
                try {
                    HttpResponse<String> resp = http.send(
                            HttpRequest.newBuilder(URI.create(url))
                                    .timeout(Duration.ofSeconds(10))
                                    .header("Content-Type", "application/json")
                                    .POST(HttpRequest.BodyPublishers.ofString(body))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() / 100 == 2) {
                        success = true;
                        error = null;
                    } else {
                        error = "HTTP " + resp.statusCode();
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    error = "Interrompu";
                    break;
                } catch (Exception e) {
                    error = e.getMessage();
                }
                if (!success && attempt < 3) {
                    try {
                        Thread.sleep(attempt * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        logWriter.record(type, body, success, error);
        if (!success) {
            LOG.warn("Webhook {} échoué : {}", type, error);
        }
    }
}
