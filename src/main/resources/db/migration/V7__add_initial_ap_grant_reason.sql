ALTER TABLE ap_transactions
    DROP CONSTRAINT ap_transactions_reason_check,
    DROP CONSTRAINT ap_transactions_reason_ref,
    DROP CONSTRAINT ap_transactions_amount_sign;

ALTER TABLE ap_transactions
    ADD CONSTRAINT ap_transactions_reason_check CHECK (
        reason IN (
            'INITIAL_GRANT', 'ATTENDANCE', 'TUTORIAL', 'BADGE',
            'DECISION_WIN', 'DECISION_LOSE', 'NEUTRAL_HIT',
            'SALARY', 'CREDIT_LOAN'
        )
    ),
    ADD CONSTRAINT ap_transactions_reason_ref CHECK (
        (reason = 'ATTENDANCE' AND ref_type = 'ATTENDANCE_REWARD') OR
        (reason = 'BADGE' AND ref_type = 'USER_BADGE') OR
        (reason IN ('DECISION_WIN', 'DECISION_LOSE', 'NEUTRAL_HIT') AND ref_type = 'DECISION') OR
        (reason = 'SALARY' AND ref_type = 'SALARY_LOG') OR
        (reason IN ('INITIAL_GRANT', 'TUTORIAL', 'CREDIT_LOAN') AND ref_type IS NULL)
    ),
    ADD CONSTRAINT ap_transactions_amount_sign CHECK (
        (reason IN (
            'INITIAL_GRANT', 'ATTENDANCE', 'TUTORIAL', 'BADGE',
            'DECISION_WIN', 'NEUTRAL_HIT', 'CREDIT_LOAN'
        ) AND amount > 0) OR
        (reason IN ('DECISION_LOSE', 'SALARY') AND amount < 0)
    );

CREATE UNIQUE INDEX idx_ap_transactions_user_initial_grant_unique
    ON ap_transactions (user_id)
    WHERE reason = 'INITIAL_GRANT';
