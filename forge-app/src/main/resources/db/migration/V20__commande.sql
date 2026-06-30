-- Commandes client : devis → … → livrée. À la livraison, facture_id pointe vers la facture générée.
CREATE SEQUENCE commande_numero_seq START 1;

CREATE TABLE commande (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    business_id UUID         NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    numero      BIGINT       NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    client_name VARCHAR(255),
    client_note VARCHAR(1000),
    due_date    TIMESTAMPTZ,
    acompte     BIGINT       NOT NULL DEFAULT 0,
    facture_id  UUID         REFERENCES facture(id) ON DELETE SET NULL,
    created_by  UUID         NOT NULL REFERENCES app_user(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_commande_business ON commande(business_id, created_at);

CREATE TABLE commande_line (
    id                  UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    commande_id         UUID          NOT NULL REFERENCES commande(id) ON DELETE CASCADE,
    item_id             UUID          NOT NULL REFERENCES item(id) ON DELETE RESTRICT,
    quantity            INTEGER       NOT NULL CHECK (quantity > 0),
    unit_price_snapshot NUMERIC(12,4) NOT NULL DEFAULT 0
);
CREATE INDEX idx_commande_line_commande ON commande_line(commande_id);
