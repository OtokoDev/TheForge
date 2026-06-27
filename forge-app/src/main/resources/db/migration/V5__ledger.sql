-- Contexte Ledger : comptes (coffres / stock) et journal de mouvements append-only.
-- Stock d'un compte et solde en septimes = projections (Σ entrées − Σ sorties).
CREATE TABLE account (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    business_id UUID         NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    kind        VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_account_business ON account(business_id);

-- Journal append-only. from nul = création / to nul = destruction / les deux = transfert.
-- Les comptes référencés ne sont pas supprimables (RESTRICT) pour préserver l'historique.
CREATE TABLE movement (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    business_id     UUID        NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    item_id         UUID        NOT NULL REFERENCES item(id) ON DELETE RESTRICT,
    quantity        INTEGER     NOT NULL CHECK (quantity > 0),
    from_account_id UUID        REFERENCES account(id) ON DELETE RESTRICT,
    to_account_id   UUID        REFERENCES account(id) ON DELETE RESTRICT,
    type            VARCHAR(20) NOT NULL,
    reference_type  VARCHAR(30),
    reference_id    UUID,
    note            VARCHAR(255),
    user_id         UUID        NOT NULL REFERENCES app_user(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (from_account_id IS NOT NULL OR to_account_id IS NOT NULL)
);
CREATE INDEX idx_movement_business ON movement(business_id, created_at);
CREATE INDEX idx_movement_from ON movement(from_account_id, item_id);
CREATE INDEX idx_movement_to ON movement(to_account_id, item_id);
