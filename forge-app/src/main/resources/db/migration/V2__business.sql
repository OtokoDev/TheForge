-- Contexte Business : les tenants et l'appartenance des utilisateurs.
CREATE TABLE business (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    nom        VARCHAR(100) NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Appartenance d'un utilisateur à un business, avec son rôle local (ADMIN | MEMBRE).
CREATE TABLE membership (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    business_id UUID        NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, business_id)
);

CREATE INDEX idx_membership_user ON membership(user_id);
CREATE INDEX idx_membership_business ON membership(business_id);
