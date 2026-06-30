-- Atelier : ordres de fabrication. À la clôture, ingrédients consommés + objet produit ajouté au stock.
CREATE SEQUENCE production_numero_seq START 1;

CREATE TABLE production_order (
    id             UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    business_id    UUID         NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    numero         BIGINT       NOT NULL,
    output_item_id UUID         NOT NULL REFERENCES item(id) ON DELETE RESTRICT,
    quantity       INTEGER      NOT NULL CHECK (quantity > 0),
    status         VARCHAR(20)  NOT NULL,
    assigned_to    UUID         REFERENCES app_user(id) ON DELETE SET NULL,
    created_by     UUID         NOT NULL REFERENCES app_user(id),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at   TIMESTAMPTZ
);
CREATE INDEX idx_production_business ON production_order(business_id, created_at);
