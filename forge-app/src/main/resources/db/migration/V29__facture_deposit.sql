-- Acompte déjà encaissé (via la commande d'origine), déduit du montant à encaisser à la validation.
ALTER TABLE facture ADD COLUMN deposit BIGINT NOT NULL DEFAULT 0;
