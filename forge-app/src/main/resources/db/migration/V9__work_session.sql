-- Contexte Billing : prises de poste (work_session). Récap figé à la fermeture.
CREATE TABLE work_session (
    id                UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    business_id       UUID          NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    user_id           UUID          NOT NULL REFERENCES app_user(id),
    opened_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    closed_at         TIMESTAMPTZ,
    tax_rate_snapshot NUMERIC(5,4)  NOT NULL DEFAULT 0,
    orders_count      INTEGER       NOT NULL DEFAULT 0,
    total_sales       BIGINT        NOT NULL DEFAULT 0,
    total_cost        NUMERIC(14,4) NOT NULL DEFAULT 0,
    total_profit      NUMERIC(14,4) NOT NULL DEFAULT 0,
    business_share    NUMERIC(14,4) NOT NULL DEFAULT 0,
    worker_share      NUMERIC(14,4) NOT NULL DEFAULT 0
);
CREATE INDEX idx_work_session_business ON work_session(business_id, opened_at);

-- Un seul poste ouvert par (business, user).
CREATE UNIQUE INDEX uq_work_session_open ON work_session(business_id, user_id) WHERE closed_at IS NULL;

-- Rattachement des factures au poste.
ALTER TABLE facture ADD CONSTRAINT fk_facture_session FOREIGN KEY (session_id) REFERENCES work_session(id);
