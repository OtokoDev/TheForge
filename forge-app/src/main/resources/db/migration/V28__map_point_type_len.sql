-- map_point.type stocke désormais l'id (UUID) du type de marqueur → élargir la colonne.
ALTER TABLE map_point ALTER COLUMN type TYPE VARCHAR(64);
