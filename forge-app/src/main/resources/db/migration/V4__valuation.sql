-- Contexte Valuation : valeur d'un item PAR BUSINESS, historisée (append-only).
-- unit_price = NUMERIC (décimales autorisées, ex. 0.1). valid_to NULL = version courante.
CREATE TABLE valuation (
    id          UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    business_id UUID          NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    item_id     UUID          NOT NULL REFERENCES item(id) ON DELETE CASCADE,
    unit_price  NUMERIC(12,4) NOT NULL CHECK (unit_price >= 0),
    valid_from  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    valid_to    TIMESTAMPTZ
);

-- Une seule valeur COURANTE par (business, item).
CREATE UNIQUE INDEX uq_valuation_current ON valuation(business_id, item_id) WHERE valid_to IS NULL;
CREATE INDEX idx_valuation_business ON valuation(business_id);
CREATE INDEX idx_valuation_item ON valuation(business_id, item_id);
