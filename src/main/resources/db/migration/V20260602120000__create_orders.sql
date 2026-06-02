CREATE TABLE orders (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id       VARCHAR(100)   NOT NULL,
    provider_name    VARCHAR(30)    NOT NULL,
    status           VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    total_amount     NUMERIC(10, 2) NOT NULL DEFAULT 0,
    notes            TEXT,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_session_id ON orders (session_id);
CREATE INDEX idx_orders_status     ON orders (status);
