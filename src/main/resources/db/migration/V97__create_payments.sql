CREATE SEQUENCE payments_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE payments (
    id             BIGINT PRIMARY KEY DEFAULT nextval('payments_id_seq'),
    public_id      UUID NOT NULL UNIQUE DEFAULT uuidv7(),
    user_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    order_id       VARCHAR(64) NOT NULL UNIQUE,
    amount         INTEGER NOT NULL CHECK (amount > 0),
    status         VARCHAR(20) NOT NULL DEFAULT 'READY'
                       CHECK (status IN ('READY', 'DONE', 'FAILED', 'CANCELED')),
    payment_key    VARCHAR(200),
    method         VARCHAR(30),
    failure_reason TEXT,
    confirmed_at   TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_user_created
    ON payments (user_id, created_at DESC);
