-- Scinde valuation en produit (par business) : valeur (coût, matières) + prix de revente.
ALTER TABLE valuation RENAME TO product;
ALTER TABLE product RENAME COLUMN unit_price TO valeur;
ALTER TABLE product ALTER COLUMN valeur DROP NOT NULL;
ALTER TABLE product ADD COLUMN prix_revente NUMERIC(12, 4);

-- Continuité : l'ancienne valeur servait aussi de prix de vente.
UPDATE product SET prix_revente = valeur;
