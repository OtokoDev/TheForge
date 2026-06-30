-- Seuils d'alerte de rupture par item et par business.
CREATE TABLE stock_threshold (
    id          UUID    NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    business_id UUID    NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    item_id     UUID    NOT NULL REFERENCES item(id) ON DELETE CASCADE,
    min_qty     INTEGER NOT NULL,
    UNIQUE (business_id, item_id)
);
