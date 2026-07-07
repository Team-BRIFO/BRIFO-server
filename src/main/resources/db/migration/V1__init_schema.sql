CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SEQUENCE users_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE policies_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE user_policies_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE notification_types_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE notifications_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE badges_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE user_badges_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE stocks_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE daily_stock_prices_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE agents_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE user_stocks_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE news_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE news_cards_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE glossary_terms_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE news_card_terms_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE user_learned_terms_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE briefings_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE decisions_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE diary_entries_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE attendance_rewards_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE ap_transactions_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE agent_salary_logs_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE external_api_call_logs_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE users (
                       id             BIGINT PRIMARY KEY DEFAULT nextval('users_id_seq'),
                       public_id      UUID NOT NULL UNIQUE DEFAULT uuidv7(),
                       provider       VARCHAR(20) NOT NULL CHECK (provider IN ('KAKAO', 'NAVER', 'APPLE')),
                       social_id      VARCHAR(100) NOT NULL,
                       nickname       VARCHAR(50) NOT NULL,
                       email          VARCHAR(255),
                       company_name   VARCHAR(100) NOT NULL DEFAULT '내 투자회사',
                       balance_ap     INTEGER NOT NULL DEFAULT 500 CHECK (balance_ap >= 0),
                       total_correct  INTEGER NOT NULL DEFAULT 0 CHECK (total_correct >= 0),
                       total_decision INTEGER NOT NULL DEFAULT 0 CHECK (total_decision >= 0),
                       onboarding_completed_at TIMESTAMP,
                       last_login_at  TIMESTAMP,
                       created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),
                       deleted_at     TIMESTAMP,
                       CONSTRAINT users_valid_total CHECK (total_correct <= total_decision)
);

CREATE UNIQUE INDEX idx_users_provider_social_id_active_unique
    ON users (provider, social_id)
    WHERE deleted_at IS NULL;

CREATE TABLE policies (
                          id          BIGINT PRIMARY KEY DEFAULT nextval('policies_id_seq'),
                          public_id   UUID NOT NULL UNIQUE DEFAULT uuidv7(),
                          title       VARCHAR(100) NOT NULL,
                          content     TEXT NOT NULL,
                          is_required BOOLEAN NOT NULL,
                          version     INTEGER NOT NULL DEFAULT 1 CHECK (version >= 1),
                          is_active   BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                          updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                          UNIQUE (title, version)
);

CREATE TABLE user_policies (
                               id         BIGINT PRIMARY KEY DEFAULT nextval('user_policies_id_seq'),
                               user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                               policy_id  BIGINT NOT NULL REFERENCES policies(id),
                               agreed_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                               UNIQUE (user_id, policy_id)
);

CREATE TABLE notification_types (
                                    id              BIGINT PRIMARY KEY DEFAULT nextval('notification_types_id_seq'),
                                    code            VARCHAR(40) NOT NULL UNIQUE
);

CREATE TABLE notifications (
                               id                   BIGINT PRIMARY KEY DEFAULT nextval('notifications_id_seq'),
                               public_id            UUID NOT NULL UNIQUE DEFAULT uuidv7(),
                               user_id              BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                               notification_type_id BIGINT NOT NULL REFERENCES notification_types(id),
                               title                VARCHAR(100) NOT NULL,
                               body                 VARCHAR(500),
                               ref_type             VARCHAR(30),
                               ref_public_id        UUID,
                               created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
                               CONSTRAINT notifications_ref_type CHECK (
                                   ref_type IS NULL OR ref_type IN (
                                                                    'DECISION',
                                                                    'AP_TRANSACTION',
                                                                    'BRIEFING',
                                                                    'AGENT',
                                                                    'ATTENDANCE_REWARD',
                                                                    'USER_BADGE',
                                                                    'SALARY_LOG',
                                                                    'POLICY'
                                       )
                                   ),
                               CONSTRAINT notifications_ref_pair CHECK (
                                   ref_type IS NOT NULL OR ref_public_id IS NULL
                                   )
);

CREATE INDEX idx_notifications_user_created
    ON notifications (user_id, created_at DESC);

CREATE TABLE badges (
                        id            BIGINT PRIMARY KEY DEFAULT nextval('badges_id_seq'),
                        public_id     UUID NOT NULL UNIQUE DEFAULT uuidv7(),
                        code          VARCHAR(40) NOT NULL UNIQUE,
                        name          VARCHAR(50) NOT NULL,
                        description   VARCHAR(255),
                        ap            INTEGER NOT NULL CHECK (ap > 0),
                        created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE user_badges (
                             id         BIGINT PRIMARY KEY DEFAULT nextval('user_badges_id_seq'),
                             user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                             badge_id   BIGINT NOT NULL REFERENCES badges(id),
                             awarded_at TIMESTAMP NOT NULL DEFAULT NOW(),
                             UNIQUE (user_id, badge_id)
);

CREATE INDEX idx_user_badges_user_awarded
    ON user_badges (user_id, awarded_at DESC);

CREATE TABLE stocks (
                        id         BIGINT PRIMARY KEY DEFAULT nextval('stocks_id_seq'),
                        public_id  UUID NOT NULL UNIQUE DEFAULT uuidv7(),
                        code       VARCHAR(10) NOT NULL UNIQUE,
                        name       VARCHAR(100) NOT NULL,
                        sector     VARCHAR(50) NOT NULL,
                        is_active  BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE daily_stock_prices (
                                    id          BIGINT PRIMARY KEY DEFAULT nextval('daily_stock_prices_id_seq'),
                                    stock_id    BIGINT NOT NULL REFERENCES stocks(id) ON DELETE RESTRICT,
                                    trade_date  DATE NOT NULL,
                                    close_price DECIMAL(12,2) NOT NULL,
                                    change_rate DECIMAL(5,2) NOT NULL,
                                    fetched_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                                    UNIQUE (stock_id, trade_date)
);

CREATE INDEX idx_daily_stock_prices_date ON daily_stock_prices (trade_date DESC);

CREATE TABLE agents (
                        id               BIGINT PRIMARY KEY DEFAULT nextval('agents_id_seq'),
                        public_id        UUID NOT NULL UNIQUE DEFAULT uuidv7(),
                        user_id          BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                        agent_type       VARCHAR(10) NOT NULL CHECK (agent_type IN ('ROOKIE', 'PRO', 'TANKER')),
                        model_name       VARCHAR(100) NOT NULL,
                        nickname         VARCHAR(50),
                        description      VARCHAR(255),
                        level            INTEGER NOT NULL DEFAULT 1 CHECK (level BETWEEN 1 AND 10),
                        exp              INTEGER NOT NULL DEFAULT 0 CHECK (exp >= 0),
                        daily_salary     INTEGER NOT NULL CHECK (daily_salary > 0),
                        total_analyses   INTEGER NOT NULL DEFAULT 0 CHECK (total_analyses >= 0),
                        correct_analyses INTEGER NOT NULL DEFAULT 0 CHECK (correct_analyses >= 0),
                        last_work_date   DATE,
                        is_active        BOOLEAN NOT NULL DEFAULT TRUE,
                        created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
                        updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),
                        UNIQUE (user_id, agent_type),
                        CONSTRAINT agents_valid_accuracy CHECK (correct_analyses <= total_analyses)
);

CREATE TABLE user_stocks (
                             id         BIGINT PRIMARY KEY DEFAULT nextval('user_stocks_id_seq'),
                             user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                             stock_id   BIGINT NOT NULL REFERENCES stocks(id),
                             created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                             updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                             deleted_at TIMESTAMP
);

CREATE UNIQUE INDEX idx_user_stocks_active_unique
    ON user_stocks (user_id, stock_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_user_stocks_user_deleted ON user_stocks (user_id, deleted_at);

CREATE TABLE news (
                      id                BIGINT PRIMARY KEY DEFAULT nextval('news_id_seq'),
                      public_id         UUID NOT NULL UNIQUE DEFAULT uuidv7(),
                      stock_id          BIGINT NOT NULL REFERENCES stocks(id),
                      source            VARCHAR(30) NOT NULL CHECK (source IN ('NAVER', 'DART', 'KRX')),
                      source_url        TEXT NOT NULL,
                      title             VARCHAR(500) NOT NULL,
                      summary           TEXT,
                      importance        DECIMAL(3,2) CHECK (importance BETWEEN 0 AND 1),
                      dedup_key         VARCHAR(255) NOT NULL UNIQUE,
                      processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                          CHECK (processing_status IN ('PENDING', 'PROCESSED', 'FAILED', 'SKIPPED')),
                      published_at      TIMESTAMP NOT NULL,
                      crawled_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_news_stock_date ON news (stock_id, published_at DESC);
CREATE INDEX idx_news_processing_status_date ON news (processing_status, published_at ASC);

CREATE TABLE news_cards (
                            id               BIGINT PRIMARY KEY DEFAULT nextval('news_cards_id_seq'),
                            public_id        UUID NOT NULL UNIQUE DEFAULT uuidv7(),
                            news_id          BIGINT NOT NULL UNIQUE REFERENCES news(id) ON DELETE RESTRICT,
                            headline         VARCHAR(80) NOT NULL,
                            points           JSONB NOT NULL,
                            keywords         JSONB NOT NULL,
                            importance_badge VARCHAR(5) CHECK (importance_badge IN ('HOT', 'MID', 'LOW')),
                            card_date        DATE NOT NULL,
                            created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_news_cards_card_date ON news_cards (card_date DESC);

CREATE TABLE glossary_terms (
                                id             BIGINT PRIMARY KEY DEFAULT nextval('glossary_terms_id_seq'),
                                public_id      UUID NOT NULL UNIQUE DEFAULT uuidv7(),
                                term           VARCHAR(80) NOT NULL UNIQUE,
                                definition     VARCHAR(200) NOT NULL,
                                category       VARCHAR(50),
                                created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE news_card_terms (
                                 id      BIGINT PRIMARY KEY DEFAULT nextval('news_card_terms_id_seq'),
                                 card_id BIGINT NOT NULL REFERENCES news_cards(id) ON DELETE RESTRICT,
                                 term_id BIGINT NOT NULL REFERENCES glossary_terms(id),
                                 surface VARCHAR(80),
                                 display_order INTEGER NOT NULL CHECK (display_order >= 0),
                                 UNIQUE (card_id, term_id),
                                 UNIQUE (card_id, display_order)
);

CREATE TABLE user_learned_terms (
                                    id         BIGINT PRIMARY KEY DEFAULT nextval('user_learned_terms_id_seq'),
                                    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                                    term_id    BIGINT NOT NULL REFERENCES glossary_terms(id),
                                    learned_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                    UNIQUE (user_id, term_id)
);

CREATE TABLE briefings (
                           id           BIGINT PRIMARY KEY DEFAULT nextval('briefings_id_seq'),
                           public_id    UUID NOT NULL UNIQUE DEFAULT uuidv7(),
                           card_id      BIGINT NOT NULL REFERENCES news_cards(id) ON DELETE RESTRICT,
                           agent_id     BIGINT NOT NULL REFERENCES agents(id) ON DELETE RESTRICT,
                           content_text TEXT,
                           headline     VARCHAR(200),
                           one_liner    VARCHAR(200),
                           direction    VARCHAR(10) CHECK (direction IN ('UP', 'DOWN', 'NEUTRAL')),
                           confidence   DECIMAL(3,2) CHECK (confidence BETWEEN 0 AND 1),
                           status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                               CHECK (status IN ('PENDING', 'ANALYZING', 'COMPLETED', 'FAILED')),
                           created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
                           updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
                           UNIQUE (card_id, agent_id)
);

CREATE INDEX idx_briefings_agent_created ON briefings (agent_id, created_at DESC);

CREATE TABLE decisions (
                           id            BIGINT PRIMARY KEY DEFAULT nextval('decisions_id_seq'),
                           public_id     UUID NOT NULL UNIQUE DEFAULT uuidv7(),
                           user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                           card_id       BIGINT NOT NULL REFERENCES news_cards(id) ON DELETE RESTRICT,
                           briefing_id   BIGINT NOT NULL UNIQUE REFERENCES briefings(id) ON DELETE RESTRICT,
                           direction     VARCHAR(10) NOT NULL CHECK (direction IN ('UP', 'DOWN', 'NEUTRAL')),
                           confidence    SMALLINT NOT NULL CHECK (confidence BETWEEN 1 AND 5),
                           is_correct    BOOLEAN,
                           created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
                           settled_at    TIMESTAMP,
                           UNIQUE (user_id, card_id),
                           CONSTRAINT decisions_valid_settlement CHECK (
                               settled_at IS NULL OR is_correct IS NOT NULL
                               )
);

CREATE INDEX idx_decisions_user_created ON decisions (user_id, created_at DESC);
CREATE INDEX idx_decisions_card_created ON decisions (card_id, created_at DESC);
CREATE INDEX idx_decisions_unsettled ON decisions (created_at) WHERE settled_at IS NULL;

CREATE TABLE diary_entries (
                               id          BIGINT PRIMARY KEY DEFAULT nextval('diary_entries_id_seq'),
                               public_id   UUID NOT NULL UNIQUE DEFAULT uuidv7(),
                               decision_id BIGINT NOT NULL UNIQUE REFERENCES decisions(id) ON DELETE RESTRICT,
                               share_count INTEGER NOT NULL DEFAULT 0 CHECK (share_count >= 0),
                               share_image_url TEXT,
                               share_image_created_at TIMESTAMP,
                               created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE attendance_rewards (
                                    id             BIGINT PRIMARY KEY DEFAULT nextval('attendance_rewards_id_seq'),
                                    user_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                                    bonus_rewarded BOOLEAN NOT NULL,
                                    consecutive_days INTEGER NOT NULL CHECK (consecutive_days BETWEEN 1 AND 7),
                                    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_attendance_rewards_user_date
    ON attendance_rewards (user_id, (created_at::date));

CREATE INDEX idx_attendance_rewards_user_created ON attendance_rewards (user_id, created_at DESC);

CREATE TABLE ap_transactions (
                                 id         BIGINT PRIMARY KEY DEFAULT nextval('ap_transactions_id_seq'),
                                 public_id  UUID NOT NULL UNIQUE DEFAULT uuidv7(),
                                 user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                                 amount     INTEGER NOT NULL CHECK (amount <> 0),
                                 reason     VARCHAR(30) NOT NULL CHECK (reason IN (
                                                                                   'ATTENDANCE', 'TUTORIAL', 'BADGE',
                                                                                   'DECISION_WIN', 'DECISION_LOSE', 'NEUTRAL_HIT',
                                                                                   'SALARY', 'CREDIT_LOAN'
                                     )),
                                 ref_type   VARCHAR(30) CHECK (ref_type IN (
                                                                            'DECISION',
                                                                            'SALARY_LOG',
                                                                            'ATTENDANCE_REWARD',
                                                                            'USER_BADGE'
                                     )),
                                 ref_id     BIGINT,
                                 created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                 CONSTRAINT ap_transactions_ref_pair CHECK (
                                     (ref_type IS NULL AND ref_id IS NULL) OR
                                     (ref_type IS NOT NULL AND ref_id IS NOT NULL)
                                     ),
                                 CONSTRAINT ap_transactions_reason_ref CHECK (
                                     (reason = 'ATTENDANCE' AND ref_type = 'ATTENDANCE_REWARD') OR
                                     (reason = 'BADGE' AND ref_type = 'USER_BADGE') OR
                                     (reason IN ('DECISION_WIN', 'DECISION_LOSE', 'NEUTRAL_HIT') AND ref_type = 'DECISION') OR
                                     (reason = 'SALARY' AND ref_type = 'SALARY_LOG') OR
                                     (reason IN ('TUTORIAL', 'CREDIT_LOAN') AND ref_type IS NULL)
                                     ),
                                 CONSTRAINT ap_transactions_amount_sign CHECK (
                                     (reason IN ('ATTENDANCE', 'TUTORIAL', 'BADGE', 'DECISION_WIN', 'NEUTRAL_HIT', 'CREDIT_LOAN') AND amount > 0) OR
                                     (reason IN ('DECISION_LOSE', 'SALARY') AND amount < 0)
                                     )
);

CREATE INDEX idx_ap_transactions_user_date ON ap_transactions (user_id, created_at DESC);

CREATE UNIQUE INDEX idx_ap_transactions_user_tutorial_unique
    ON ap_transactions (user_id)
    WHERE reason = 'TUTORIAL';

CREATE UNIQUE INDEX idx_ap_transactions_user_credit_loan_unique
    ON ap_transactions (user_id)
    WHERE reason = 'CREDIT_LOAN';

CREATE TABLE agent_salary_logs (
                                   id            BIGINT PRIMARY KEY DEFAULT nextval('agent_salary_logs_id_seq'),
                                   agent_id      BIGINT NOT NULL REFERENCES agents(id) ON DELETE RESTRICT,
                                   salary_amount INTEGER NOT NULL CHECK (salary_amount > 0),
                                   salary_date   DATE NOT NULL,
                                   status        VARCHAR(30) NOT NULL CHECK (status IN (
                                                                                        'PAID',
                                                                                        'PAID_WITH_CREDIT',
                                                                                        'INSUFFICIENT_BALANCE'
                                       )),
                                   created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
                                   UNIQUE (agent_id, salary_date)
);

CREATE INDEX idx_salary_date ON agent_salary_logs (salary_date DESC);

CREATE TABLE external_api_call_logs (
                                        id               BIGINT PRIMARY KEY DEFAULT nextval('external_api_call_logs_id_seq'),
                                        api_name         VARCHAR(100) NOT NULL,
                                        provider         VARCHAR(100),
                                        request_payload  JSONB,
                                        response_payload JSONB,
                                        status           VARCHAR(20) CHECK (status IN ('SUCCESS', 'FAIL', 'TIMEOUT')),
                                        http_status_code INTEGER,
                                        retry_count      INTEGER NOT NULL DEFAULT 0 CHECK (retry_count >= 0),
                                        latency_ms       INTEGER CHECK (latency_ms >= 0),
                                        called_at        TIMESTAMP NOT NULL DEFAULT NOW()
);
