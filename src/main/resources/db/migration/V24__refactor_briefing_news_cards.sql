ALTER TABLE news_cards
    ADD COLUMN display_date DATE NOT NULL;

CREATE INDEX idx_news_cards_display_date
    ON news_cards (display_date);

DROP INDEX idx_briefings_card_agent_active_unique;
DROP INDEX idx_ap_transactions_briefing_salary_unique;

CREATE INDEX idx_ap_transactions_briefing_salary
    ON ap_transactions (target_id, reason)
    WHERE target_type = 'BRIEFING'
      AND reason IN ('SALARY', 'SALARY_REFUND');

ALTER TABLE briefings
    DROP COLUMN card_id,
    ADD COLUMN summary TEXT,
    ADD COLUMN personal_comment TEXT;

CREATE SEQUENCE briefing_news_cards_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE briefing_news_cards (
    id          BIGINT PRIMARY KEY DEFAULT nextval('briefing_news_cards_id_seq'),
    briefing_id BIGINT NOT NULL REFERENCES briefings(id) ON DELETE RESTRICT,
    card_id     BIGINT NOT NULL REFERENCES news_cards(id) ON DELETE RESTRICT,
    UNIQUE (briefing_id, card_id)
);

CREATE INDEX idx_briefing_news_cards_card_briefing
    ON briefing_news_cards (card_id, briefing_id);
