package com.bryan.forge.notifications.datamodel;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Journal des envois de webhooks (audit). */
@Entity
@Table(name = "webhook_log")
public class WebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 500)
    private String error;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected WebhookLog() {}

    public WebhookLog(String type, String payload, boolean success, String error) {
        this.type = type;
        this.payload = payload;
        this.success = success;
        this.error = error;
    }

    public UUID getId()        { return id; }
    public String getType()    { return type; }
    public String getPayload() { return payload; }
    public boolean isSuccess() { return success; }
    public String getError()   { return error; }
    public Instant getCreatedAt() { return createdAt; }
}
