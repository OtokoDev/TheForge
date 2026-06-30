-- Solde de stock matérialisé (qty par compte × item). Remplace le re-calcul de tout
-- l'historique des mouvements à chaque lecture. Maintenu par l'application à chaque écriture.
CREATE TABLE stock_balance (
    id         UUID   NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    account_id UUID   NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    item_id    UUID   NOT NULL REFERENCES item(id) ON DELETE RESTRICT,
    qty        BIGINT NOT NULL,
    UNIQUE (account_id, item_id)
);
CREATE INDEX idx_stock_balance_account ON stock_balance(account_id);

-- Backfill depuis les mouvements existants : Σ(entrées) − Σ(sorties) par compte × item.
INSERT INTO stock_balance (id, account_id, item_id, qty)
SELECT gen_random_uuid(), account_id, item_id, SUM(q)
FROM (
    SELECT to_account_id   AS account_id, item_id,  quantity::bigint AS q FROM movement WHERE to_account_id   IS NOT NULL
    UNION ALL
    SELECT from_account_id AS account_id, item_id, -quantity::bigint AS q FROM movement WHERE from_account_id IS NOT NULL
) m
GROUP BY account_id, item_id;
