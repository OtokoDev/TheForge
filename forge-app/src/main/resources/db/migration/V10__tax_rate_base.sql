-- Assiette de la taxe : PROFIT (sur le bénéfice = comportement historique) ou REVENUE (sur le CA).
ALTER TABLE tax_rate ADD COLUMN base VARCHAR(16) NOT NULL DEFAULT 'PROFIT';
