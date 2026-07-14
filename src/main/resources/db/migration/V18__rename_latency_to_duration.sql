ALTER TABLE external_api_call_logs
    RENAME COLUMN latency_ms TO duration_ms;

ALTER TABLE external_api_call_logs
    ALTER COLUMN duration_ms TYPE BIGINT;
