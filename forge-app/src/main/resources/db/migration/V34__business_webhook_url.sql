-- Webhook Discord par business (canal de la faction). Si vide → fallback URLs globales (env).
ALTER TABLE business ADD COLUMN webhook_url VARCHAR(500);
