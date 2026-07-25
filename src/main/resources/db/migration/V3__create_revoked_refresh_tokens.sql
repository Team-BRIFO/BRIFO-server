CREATE SEQUENCE revoked_refresh_tokens_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE revoked_refresh_tokens (
    id             BIGINT PRIMARY KEY DEFAULT nextval('revoked_refresh_tokens_id_seq'),
    token_id       VARCHAR(36) NOT NULL UNIQUE,
    user_public_id UUID NOT NULL,
    expires_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_revoked_refresh_tokens_expires_at
    ON revoked_refresh_tokens (expires_at);
