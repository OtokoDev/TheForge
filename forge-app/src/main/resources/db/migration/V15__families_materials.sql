-- Deux dimensions de classement configurables par SYSTEM : famille + matériau.
-- Remplace l'ancien enum item.type.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE taxon (
    id      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    kind    varchar(10)  NOT NULL,   -- FAMILY | MATERIAL
    nom     varchar(60)  NOT NULL,
    ordre   integer      NOT NULL DEFAULT 0,
    couleur varchar(9),
    CONSTRAINT uq_taxon_kind_nom UNIQUE (kind, nom)
);

ALTER TABLE item ADD COLUMN family_id   uuid REFERENCES taxon(id) ON DELETE SET NULL;
ALTER TABLE item ADD COLUMN material_id uuid REFERENCES taxon(id) ON DELETE SET NULL;
ALTER TABLE item DROP COLUMN type;
