-- Contexte Billing : factures (BROUILLON → VALIDEE) et lignes.
-- numero = séquence globale ; total_amount = septimes effectifs (INTEGER) ; le reste
-- (coûts, parts) = figures comptables (NUMERIC). Arrondi par excès appliqué à la ligne.
CREATE SEQUENCE facture_numero_seq START 1;

CREATE TABLE facture (
    id                UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    business_id       UUID         NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    numero            BIGINT       NOT NULL,
    session_id        UUID,
    status            VARCHAR(20)  NOT NULL,
    total_amount      BIGINT       NOT NULL DEFAULT 0,
    total_cost        NUMERIC(14,4) NOT NULL DEFAULT 0,
    total_profit      NUMERIC(14,4) NOT NULL DEFAULT 0,
    tax_rate_snapshot NUMERIC(5,4)  NOT NULL DEFAULT 0,
    business_share    NUMERIC(14,4) NOT NULL DEFAULT 0,
    worker_share      NUMERIC(14,4) NOT NULL DEFAULT 0,
    client_note       VARCHAR(1000),
    internal_note     VARCHAR(1000),
    created_by        UUID         NOT NULL REFERENCES app_user(id),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    validated_at      TIMESTAMPTZ
);
CREATE INDEX idx_facture_business ON facture(business_id, created_at);

CREATE TABLE facture_line (
    id                  UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    facture_id          UUID          NOT NULL REFERENCES facture(id) ON DELETE CASCADE,
    item_id             UUID          NOT NULL REFERENCES item(id) ON DELETE RESTRICT,
    quantity            INTEGER       NOT NULL CHECK (quantity > 0),
    unit_price_snapshot NUMERIC(12,4) NOT NULL DEFAULT 0,
    unit_cost_snapshot  NUMERIC(12,4) NOT NULL DEFAULT 0,
    line_total          BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX idx_facture_line_facture ON facture_line(facture_id);
