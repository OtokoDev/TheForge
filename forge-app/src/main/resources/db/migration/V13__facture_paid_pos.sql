-- Facturation : nom du client + statut payé/à crédit.
ALTER TABLE facture ADD COLUMN client_name VARCHAR(100);
ALTER TABLE facture ADD COLUMN paid BOOLEAN NOT NULL DEFAULT FALSE;

-- POS : comptes par défaut du business (d'où sort la marchandise / où entrent les septims).
ALTER TABLE business ADD COLUMN default_stock_account_id  UUID REFERENCES account(id) ON DELETE SET NULL;
ALTER TABLE business ADD COLUMN default_coffre_account_id UUID REFERENCES account(id) ON DELETE SET NULL;
