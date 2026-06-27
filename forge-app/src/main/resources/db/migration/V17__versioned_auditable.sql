-- Verrou optimiste + traçabilité sur les entités mutables du catalogue.
ALTER TABLE item    ADD COLUMN version integer NOT NULL DEFAULT 0,
                    ADD COLUMN updated_at timestamptz,
                    ADD COLUMN created_by uuid,
                    ADD COLUMN modified_by uuid;

ALTER TABLE taxon   ADD COLUMN version integer NOT NULL DEFAULT 0,
                    ADD COLUMN updated_at timestamptz,
                    ADD COLUMN created_by uuid,
                    ADD COLUMN modified_by uuid;

ALTER TABLE product ADD COLUMN version integer NOT NULL DEFAULT 0,
                    ADD COLUMN updated_at timestamptz,
                    ADD COLUMN created_by uuid,
                    ADD COLUMN modified_by uuid;
