-- Contexte IAM : utilisateurs de la plateforme. L'identité provient de Discord (OAuth2).
CREATE TABLE app_user (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    discord_id   VARCHAR(32)  NOT NULL UNIQUE,
    username     VARCHAR(100) NOT NULL,
    in_game_name VARCHAR(100),
    avatar       VARCHAR(255),
    global_role  VARCHAR(20)  NOT NULL DEFAULT 'NONE',
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_app_user_discord_id ON app_user(discord_id);
