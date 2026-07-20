ALTER TABLE external_api_call_logs
    RENAME COLUMN latency_ms TO duration_ms;

ALTER TABLE external_api_call_logs
    ALTER COLUMN duration_ms TYPE BIGINT;

ALTER TABLE external_api_call_logs
    RENAME COLUMN request_payload TO request_payload_redacted;

ALTER TABLE external_api_call_logs
    RENAME COLUMN response_payload TO response_payload_redacted;

ALTER TABLE external_api_call_logs
    RENAME COLUMN http_status_code TO response_status_code;

ALTER TABLE external_api_call_logs
    RENAME COLUMN called_at TO created_at;

ALTER TABLE external_api_call_logs
ALTER COLUMN provider TYPE VARCHAR(50),
    ALTER COLUMN provider SET NOT NULL,
    ALTER COLUMN status TYPE VARCHAR(30),
    ALTER COLUMN status SET NOT NULL,
    ADD COLUMN user_id BIGINT,
    ADD COLUMN stock_id BIGINT,
    ADD COLUMN news_id BIGINT,
    ADD COLUMN briefing_id BIGINT,
    ADD COLUMN idempotency_key VARCHAR(150),
    ADD COLUMN error_message TEXT,
    ADD COLUMN total_tokens INTEGER,
    ADD COLUMN estimated_cost_krw DECIMAL(12, 4),
    ADD COLUMN requested_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ADD COLUMN responded_at TIMESTAMP;
