-- Types de marqueurs configurables par business (compagnies) : libellé, couleur, image optionnelle.
CREATE TABLE map_marker_type (
    id             UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    business_id    UUID        NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    label          VARCHAR(60) NOT NULL,
    color          VARCHAR(16) NOT NULL,
    image_data_url TEXT
);
CREATE INDEX idx_map_marker_type_business ON map_marker_type(business_id);
