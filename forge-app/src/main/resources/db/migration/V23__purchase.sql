-- Achats fournisseurs : septimes sortis du coffre, matières entrées au stock.
CREATE SEQUENCE purchase_numero_seq START 1;

CREATE TABLE purchase (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    business_id   UUID         NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    numero        BIGINT       NOT NULL,
    supplier_name VARCHAR(255),
    total         BIGINT       NOT NULL DEFAULT 0,
    created_by    UUID         NOT NULL REFERENCES app_user(id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_purchase_business ON purchase(business_id, created_at);

CREATE TABLE purchase_line (
    id          UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    purchase_id UUID          NOT NULL REFERENCES purchase(id) ON DELETE CASCADE,
    item_id     UUID          NOT NULL REFERENCES item(id) ON DELETE RESTRICT,
    quantity    INTEGER       NOT NULL CHECK (quantity > 0),
    unit_cost   NUMERIC(12,4) NOT NULL DEFAULT 0
);
CREATE INDEX idx_purchase_line_purchase ON purchase_line(purchase_id);
