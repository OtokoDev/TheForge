-- Verrou optimiste + traçabilité sur les autres entités mutables.
ALTER TABLE business   ADD COLUMN version integer NOT NULL DEFAULT 0,
                       ADD COLUMN updated_at timestamptz,
                       ADD COLUMN created_by uuid,
                       ADD COLUMN modified_by uuid;

ALTER TABLE membership ADD COLUMN version integer NOT NULL DEFAULT 0,
                       ADD COLUMN updated_at timestamptz,
                       ADD COLUMN created_by uuid,
                       ADD COLUMN modified_by uuid;

ALTER TABLE tax_rate   ADD COLUMN version integer NOT NULL DEFAULT 0,
                       ADD COLUMN updated_at timestamptz,
                       ADD COLUMN created_by uuid,
                       ADD COLUMN modified_by uuid;
