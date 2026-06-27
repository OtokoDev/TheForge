-- Contexte Billing : taux de taxe PAR BUSINESS, historisé (append-only).
-- rate = fraction du bénéfice prélevée pour la part business (0..1). valid_to NULL = courant.
CREATE TABLE tax_rate (
    id          UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    business_id UUID          NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    rate        NUMERIC(5,4)  NOT NULL CHECK (rate >= 0 AND rate <= 1),
    valid_from  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    valid_to    TIMESTAMPTZ
);

-- Un seul taux COURANT par business.
CREATE UNIQUE INDEX uq_tax_rate_current ON tax_rate(business_id) WHERE valid_to IS NULL;
