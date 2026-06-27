-- Contexte Treasury : créances envers les farmeurs (append-only). Reste dû = Σ CREDIT − Σ PAIEMENT.
CREATE TABLE creance_entry (
    id             UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    business_id    UUID        NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    farmer_user_id UUID        NOT NULL REFERENCES app_user(id),
    type           VARCHAR(10) NOT NULL,
    amount         BIGINT      NOT NULL CHECK (amount > 0),
    reference      VARCHAR(255),
    created_by     UUID        NOT NULL REFERENCES app_user(id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_creance_business_farmer ON creance_entry(business_id, farmer_user_id);
