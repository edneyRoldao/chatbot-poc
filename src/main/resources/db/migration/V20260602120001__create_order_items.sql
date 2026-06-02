CREATE TABLE order_items (
    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID           NOT NULL REFERENCES orders (id),
    pizza_name  VARCHAR(200)   NOT NULL,
    pizza_size  VARCHAR(20)    NOT NULL,
    quantity    INT            NOT NULL DEFAULT 1,
    unit_price  NUMERIC(10, 2) NOT NULL,
    total_price NUMERIC(10, 2) NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
