CREATE SEQUENCE pending_user_stocks_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE pending_user_stocks (
    id           BIGINT PRIMARY KEY DEFAULT nextval('pending_user_stocks_id_seq'),
    user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    stock_id     BIGINT NOT NULL REFERENCES stocks(id) ON DELETE RESTRICT,
    effective_at TIMESTAMP NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, stock_id)
);

CREATE INDEX idx_pending_user_stocks_effective_user
    ON pending_user_stocks (effective_at, user_id);

ALTER TABLE agents
    ALTER COLUMN description DROP NOT NULL;
