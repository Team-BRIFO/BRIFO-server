CREATE INDEX idx_decision_results_created_at
    ON decision_results (created_at);

CREATE INDEX idx_ap_transactions_decision_target
    ON ap_transactions (target_id)
    WHERE target_type = 'DECISION';
