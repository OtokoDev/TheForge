-- Modèle taxe revu : rate = part forgeron (sur le CA). Taxe ville = forfait hebdo + % du CA après paie forgerons.
ALTER TABLE tax_rate ADD COLUMN city_fixed BIGINT NOT NULL DEFAULT 0;
ALTER TABLE tax_rate ADD COLUMN city_rate NUMERIC(5,4) NOT NULL DEFAULT 0;
