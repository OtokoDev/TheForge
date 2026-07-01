-- Commerce inter-business (CDC §6.7) : proposition d'échange marchandise contre septims,
-- créée par le business vendeur, acceptée/refusée par l'acheteur. Exécution atomique à l'acceptation.
CREATE SEQUENCE trade_numero_seq;

CREATE TABLE trade (
    id               UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    numero           BIGINT       NOT NULL,
    from_business_id UUID         NOT NULL REFERENCES business(id),
    to_business_id   UUID         NOT NULL REFERENCES business(id),
    status           VARCHAR(20)  NOT NULL DEFAULT 'PROPOSEE',
    septims          BIGINT       NOT NULL DEFAULT 0,
    note             VARCHAR(500),
    created_by       UUID         NOT NULL REFERENCES app_user(id),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    decided_by       UUID,
    decided_at       TIMESTAMPTZ
);

CREATE TABLE trade_line (
    id       UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    trade_id UUID NOT NULL REFERENCES trade(id),
    item_id  UUID NOT NULL REFERENCES item(id),
    quantity INT  NOT NULL
);

CREATE INDEX idx_trade_from ON trade(from_business_id);
CREATE INDEX idx_trade_to ON trade(to_business_id);
CREATE INDEX idx_trade_line_trade ON trade_line(trade_id);
