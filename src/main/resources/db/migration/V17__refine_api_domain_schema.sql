ALTER TABLE policies
    ALTER COLUMN version TYPE NUMERIC(5, 2)
        USING version::NUMERIC(5, 2),
    ALTER COLUMN version SET DEFAULT 1.0;

ALTER TABLE user_policies
    ADD COLUMN revoked_at TIMESTAMP,
    DROP CONSTRAINT user_policies_user_id_policy_id_key,
    ADD CONSTRAINT user_policies_valid_consent_period CHECK (
        revoked_at IS NULL OR revoked_at >= agreed_at
    );

CREATE UNIQUE INDEX idx_user_policies_active_unique
    ON user_policies (user_id, policy_id)
    WHERE revoked_at IS NULL;

ALTER TABLE notifications DROP CONSTRAINT notifications_ref_target;
ALTER TABLE notifications DROP CONSTRAINT notifications_ref_type;

ALTER TABLE briefings DROP CONSTRAINT briefings_card_id_agent_id_key;

CREATE UNIQUE INDEX idx_briefings_card_agent_active_unique
    ON briefings (card_id, agent_id)
    WHERE status <> 'FAILED';

ALTER TABLE agents
    DROP COLUMN last_work_date,
    DROP COLUMN is_active;

DROP INDEX idx_news_cards_card_date;
ALTER TABLE news_cards DROP COLUMN card_date;

DROP INDEX idx_user_stocks_active_unique;
DROP INDEX idx_user_stocks_user_deleted;

ALTER TABLE user_stocks
    DROP COLUMN deleted_at,
    ADD CONSTRAINT user_stocks_user_stock_unique UNIQUE (user_id, stock_id);

CREATE TABLE decision_results (
    decision_id         BIGINT PRIMARY KEY REFERENCES decisions(id) ON DELETE RESTRICT,
    daily_stock_price_id BIGINT NOT NULL REFERENCES daily_stock_prices(id) ON DELETE RESTRICT,
    is_correct          BOOLEAN NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE decisions
    DROP CONSTRAINT decisions_valid_settlement,
    DROP COLUMN is_correct,
    DROP COLUMN settled_at;

ALTER TABLE ap_transactions
    DROP CONSTRAINT ap_transactions_amount_check,
    DROP CONSTRAINT ap_transactions_amount_sign,
    DROP CONSTRAINT ap_transactions_reason_ref,
    DROP CONSTRAINT ap_transactions_ref_type_check,
    DROP CONSTRAINT ap_transactions_reason_check,
    DROP CONSTRAINT ap_transactions_ref_pair;

DROP TABLE agent_salary_logs;
DROP SEQUENCE agent_salary_logs_id_seq;

ALTER TABLE users
    ALTER COLUMN nickname DROP NOT NULL;

ALTER TABLE user_learned_terms
    ADD COLUMN public_id UUID NOT NULL UNIQUE DEFAULT uuidv7();

CREATE INDEX idx_user_learned_terms_user_public_id
    ON user_learned_terms (user_id, public_id DESC);

ALTER TABLE badges
    RENAME COLUMN ap TO reward_ap;

ALTER TABLE notifications
    RENAME COLUMN ref_type TO target_type;

ALTER TABLE notifications
    RENAME COLUMN ref_public_id TO target_public_id;

CREATE INDEX idx_notifications_user_public_id
    ON notifications (user_id, public_id DESC);

ALTER TABLE ap_transactions
    RENAME COLUMN ref_type TO target_type;

ALTER TABLE ap_transactions
    RENAME COLUMN ref_id TO target_id;

CREATE UNIQUE INDEX idx_ap_transactions_briefing_salary_unique
    ON ap_transactions (target_id, reason)
    WHERE target_type = 'BRIEFING'
      AND reason IN ('SALARY', 'SALARY_REFUND');

ALTER TABLE briefings
    DROP CONSTRAINT briefings_confidence_check;

ALTER TABLE briefings
    RENAME COLUMN confidence TO confidence_rate;

ALTER TABLE briefings
    ALTER COLUMN confidence_rate TYPE SMALLINT
        USING ROUND(confidence_rate * 100)::SMALLINT,
    ADD CONSTRAINT briefings_confidence_rate_range CHECK (
        confidence_rate BETWEEN 0 AND 100
    );

ALTER TABLE decisions
    RENAME COLUMN confidence TO confidence_level;

-- Enum membership and cross-column business rules are validated by the application.
-- Keep structural constraints (PK/FK/UNIQUE/NOT NULL) and simple value ranges in the DB.
ALTER TABLE users
    DROP CONSTRAINT users_provider_check,
    DROP CONSTRAINT users_valid_total,
    DROP COLUMN total_correct,
    DROP COLUMN total_decision;

ALTER TABLE agents
    DROP CONSTRAINT agents_agent_type_check,
    DROP CONSTRAINT agents_valid_accuracy;

ALTER TABLE news
    DROP CONSTRAINT news_source_check,
    DROP CONSTRAINT news_processing_status_check;

ALTER TABLE news_cards DROP CONSTRAINT news_cards_importance_badge_check;

ALTER TABLE briefings
    DROP CONSTRAINT briefings_direction_check,
    DROP CONSTRAINT briefings_status_check;

ALTER TABLE decisions DROP CONSTRAINT decisions_direction_check;
ALTER TABLE external_api_call_logs DROP CONSTRAINT external_api_call_logs_status_check;

ALTER TABLE agents
    ALTER COLUMN nickname SET NOT NULL,
    ALTER COLUMN description SET NOT NULL;

ALTER TABLE news_cards
    ALTER COLUMN importance_badge SET NOT NULL;

ALTER TABLE glossary_terms
    ALTER COLUMN category SET NOT NULL;

ALTER TABLE diary_entries
    DROP COLUMN share_count;

ALTER TABLE attendance_rewards
    DROP COLUMN bonus_rewarded;

ALTER TABLE users
    ALTER COLUMN balance_ap SET DEFAULT 0;

CREATE UNIQUE INDEX idx_ap_transactions_user_initial_grant_unique
    ON ap_transactions (user_id)
    WHERE reason = 'INITIAL_GRANT';

ALTER TABLE agents
    DROP COLUMN total_analyses,
    DROP COLUMN correct_analyses;

ALTER TABLE decisions
    DROP COLUMN user_id,
    DROP COLUMN card_id;

ALTER TABLE daily_stock_prices
    RENAME COLUMN close_price TO price;
