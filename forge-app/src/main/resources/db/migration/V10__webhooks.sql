-- Webhooks Discord : toggle par utilisateur + journal des envois.
ALTER TABLE app_user ADD COLUMN webhooks_enabled BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE webhook_log (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    type       VARCHAR(30)  NOT NULL,
    payload    TEXT         NOT NULL,
    success    BOOLEAN      NOT NULL,
    error      VARCHAR(500),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_webhook_log_created ON webhook_log(created_at);
