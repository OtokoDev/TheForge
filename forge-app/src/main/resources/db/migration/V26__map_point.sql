-- Points d'intérêt RP de la carte de Bordeciel, par business (compagnies). x/y = pixels natifs.
CREATE TABLE map_point (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    business_id UUID         NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    type        VARCHAR(30)  NOT NULL,
    label       VARCHAR(120) NOT NULL,
    x           INTEGER      NOT NULL,
    y           INTEGER      NOT NULL,
    note        VARCHAR(500),
    created_by  UUID         NOT NULL REFERENCES app_user(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_map_point_business ON map_point(business_id);
