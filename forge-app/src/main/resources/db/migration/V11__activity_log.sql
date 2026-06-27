-- Main courante : journal d'activité append-only (UUID bruts, indépendant des FK).
CREATE TABLE activity_log (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    business_id UUID,
    user_id     UUID         NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    details     VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_activity_business ON activity_log(business_id, created_at);
