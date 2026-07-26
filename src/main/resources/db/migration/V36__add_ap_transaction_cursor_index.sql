CREATE INDEX idx_ap_transactions_user_public_id
    ON ap_transactions (user_id, public_id DESC);
