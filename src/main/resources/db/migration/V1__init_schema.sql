CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
                       id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       public_id      UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
                       provider       VARCHAR(20) NOT NULL CHECK (provider IN ('KAKAO', 'NAVER', 'APPLE')),
                       social_id      VARCHAR(100) NOT NULL,
                       nickname       VARCHAR(50) NOT NULL,
                       email          VARCHAR(255),
                       company_name   VARCHAR(100) NOT NULL DEFAULT '내 투자회사',
                       balance_ap     INTEGER NOT NULL DEFAULT 500 CHECK (balance_ap >= 0),
                       total_correct  INTEGER NOT NULL DEFAULT 0 CHECK (total_correct >= 0),
                       total_decision INTEGER NOT NULL DEFAULT 0 CHECK (total_decision >= 0),
                       credit_used    BOOLEAN NOT NULL DEFAULT FALSE,
                       last_login_at  TIMESTAMP,
                       created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),
                       deleted_at     TIMESTAMP,
                       UNIQUE (provider, social_id),
                       CONSTRAINT users_valid_total CHECK (total_correct <= total_decision)
);

CREATE TABLE stocks (
                        id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        stock_code VARCHAR(10) NOT NULL UNIQUE,
                        name       VARCHAR(100) NOT NULL,
                        sector     VARCHAR(50) NOT NULL,
                        is_active  BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE agents (
                        id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        public_id        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
                        owner_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        agent_type       VARCHAR(10) NOT NULL CHECK (agent_type IN ('ROOKIE', 'PRO', 'TANKER')),
                        nickname         VARCHAR(50),
                        level            INTEGER NOT NULL DEFAULT 1 CHECK (level BETWEEN 1 AND 10),
                        exp              INTEGER NOT NULL DEFAULT 0 CHECK (exp >= 0),
                        daily_salary     INTEGER NOT NULL CHECK (daily_salary > 0),
                        total_analyses   INTEGER NOT NULL DEFAULT 0 CHECK (total_analyses >= 0),
                        correct_analyses INTEGER NOT NULL DEFAULT 0 CHECK (correct_analyses >= 0),
                        last_work_date   DATE,
                        is_active        BOOLEAN NOT NULL DEFAULT TRUE,
                        created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
                        updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),
                        UNIQUE (owner_id, agent_type),
                        CONSTRAINT agents_valid_accuracy CHECK (correct_analyses <= total_analyses)
);

CREATE TABLE user_stocks (
                             id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             stock_id   BIGINT NOT NULL REFERENCES stocks(id),
                             created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                             updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                             deleted_at TIMESTAMP,
                             UNIQUE (user_id, stock_id)
);

CREATE INDEX idx_user_stocks_user_deleted ON user_stocks (user_id, deleted_at);

CREATE TABLE news (
                      id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                      public_id         UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
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
                            id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            public_id        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
                            news_id          BIGINT NOT NULL UNIQUE REFERENCES news(id) ON DELETE CASCADE,
                            headline         VARCHAR(80) NOT NULL,
                            points           JSONB NOT NULL,
                            importance_badge VARCHAR(5) CHECK (importance_badge IN ('HOT', 'MID', 'LOW')),
                            card_date        DATE NOT NULL,
                            created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_news_cards_card_date ON news_cards (card_date DESC);

CREATE TABLE glossary_terms (
                                id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                term           VARCHAR(80) NOT NULL UNIQUE,
                                definition     VARCHAR(200) NOT NULL,
                                category       VARCHAR(50),
                                exposure_count INTEGER NOT NULL DEFAULT 0 CHECK (exposure_count >= 0),
                                created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE news_card_terms (
                                 id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                 card_id BIGINT NOT NULL REFERENCES news_cards(id) ON DELETE CASCADE,
                                 term_id BIGINT NOT NULL REFERENCES glossary_terms(id),
                                 surface VARCHAR(80),
                                 UNIQUE (card_id, term_id)
);

CREATE TABLE user_learned_terms (
                                    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                    term_id    BIGINT NOT NULL REFERENCES glossary_terms(id),
                                    learned_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                    UNIQUE (user_id, term_id)
);

CREATE TABLE briefings (
                           id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           public_id    UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
                           card_id      BIGINT NOT NULL REFERENCES news_cards(id) ON DELETE CASCADE,
                           agent_id     BIGINT NOT NULL REFERENCES agents(id) ON DELETE CASCADE,
                           content_text TEXT NOT NULL,
                           headline     VARCHAR(200),
                           direction    VARCHAR(10) CHECK (direction IN ('UP', 'DOWN', 'NEUTRAL')),
                           probability  DECIMAL(3,2) CHECK (probability BETWEEN 0 AND 1),
                           tokens_used  INTEGER CHECK (tokens_used >= 0),
                           created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
                           UNIQUE (card_id, agent_id)
);

CREATE TABLE decisions (
                           id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           public_id     UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
                           user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           briefing_id   BIGINT NOT NULL REFERENCES briefings(id) ON DELETE CASCADE,
                           direction     VARCHAR(10) NOT NULL CHECK (direction IN ('UP', 'DOWN', 'NEUTRAL')),
                           confidence    SMALLINT NOT NULL CHECK (confidence BETWEEN 1 AND 5),
                           reasoning     TEXT,
                           is_correct    BOOLEAN,
                           actual_change DECIMAL(5,2),
                           ap_delta      INTEGER NOT NULL DEFAULT 0,
                           created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
                           settled_at    TIMESTAMP,
                           UNIQUE (user_id, briefing_id),
                           CONSTRAINT decisions_valid_settlement CHECK (
                               (settled_at IS NULL AND is_correct IS NULL) OR
                               (settled_at IS NOT NULL AND is_correct IS NOT NULL)
                               )
);

CREATE INDEX idx_decisions_user_date ON decisions (user_id, created_at DESC);
CREATE INDEX idx_decisions_unsettled ON decisions (created_at) WHERE settled_at IS NULL;

CREATE TABLE diary_entries (
                               id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               public_id   UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
                               user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               decision_id BIGINT NOT NULL UNIQUE REFERENCES decisions(id) ON DELETE CASCADE,
                               memo        TEXT,
                               share_count INTEGER NOT NULL DEFAULT 0 CHECK (share_count >= 0),
                               created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_diary_entries_user_date ON diary_entries (user_id, created_at DESC);

CREATE TABLE ap_transactions (
                                 id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                 public_id  UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
                                 user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                 amount     INTEGER NOT NULL CHECK (amount <> 0),
                                 reason     VARCHAR(30) NOT NULL CHECK (reason IN (
                                                                                   'ATTENDANCE', 'SIGNUP_BONUS', 'TUTORIAL',
                                                                                   'DECISION_WIN', 'DECISION_LOSE', 'NEUTRAL_HIT',
                                                                                   'SALARY', 'CREDIT_LOAN'
                                     )),
                                 ref_type   VARCHAR(30) CHECK (ref_type IN ('DECISION', 'SALARY_LOG')),
                                 ref_id     BIGINT,
                                 created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                 CONSTRAINT ap_transactions_ref_pair CHECK (
                                     (ref_type IS NULL AND ref_id IS NULL) OR
                                     (ref_type IS NOT NULL AND ref_id IS NOT NULL)
                                     )
);

CREATE INDEX idx_ap_transactions_user_date ON ap_transactions (user_id, created_at DESC);

CREATE TABLE agent_salary_logs (
                                   id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                   agent_id      BIGINT NOT NULL REFERENCES agents(id) ON DELETE CASCADE,
                                   user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                   salary_amount INTEGER NOT NULL CHECK (salary_amount > 0),
                                   payment_date  DATE NOT NULL,
                                   is_paid       BOOLEAN NOT NULL,
                                   reason        VARCHAR(30) NOT NULL CHECK (reason IN (
                                                                                        'PAID',
                                                                                        'PAID_WITH_CREDIT',
                                                                                        'INSUFFICIENT_BALANCE'
                                       )),
                                   created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
                                   UNIQUE (agent_id, payment_date)
);

CREATE INDEX idx_salary_date ON agent_salary_logs (payment_date DESC);
CREATE INDEX idx_salary_agent ON agent_salary_logs (agent_id, payment_date DESC);

CREATE TABLE external_api_call_logs (
                                        id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
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
