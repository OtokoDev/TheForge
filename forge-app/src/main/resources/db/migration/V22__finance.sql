-- Finance : versements aux forgerons (paie) et dépenses du business. Septimes sortis du coffre.
CREATE TABLE payout (
    id               UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    business_id      UUID         NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    forgeron_user_id UUID         NOT NULL REFERENCES app_user(id),
    amount           BIGINT       NOT NULL,
    note             VARCHAR(500),
    created_by       UUID         NOT NULL REFERENCES app_user(id),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_payout_business ON payout(business_id, created_at);

CREATE TABLE expense (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    business_id UUID         NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    label       VARCHAR(255) NOT NULL,
    amount      BIGINT       NOT NULL,
    category    VARCHAR(60),
    created_by  UUID         NOT NULL REFERENCES app_user(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_expense_business ON expense(business_id, created_at);
