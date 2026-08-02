INSERT INTO policies (code, title, content, is_required, version)
VALUES ('AGE_14_OR_OLDER', '만 14세 이상 동의', 'test', TRUE, 1.0),
       ('TERMS_OF_SERVICE', '서비스 이용약관', 'test', TRUE, 1.0),
       ('PRIVACY_POLICY', '개인정보 처리방침', 'test', TRUE, 1.0),
       ('INVESTMENT_INFORMATION_NOTICE', '투자 정보 안내', 'test', TRUE, 1.0),
       ('MARKETING_COMMUNICATION_CONSENT', '마케팅 정보 수신 동의', 'test', FALSE, 1.0)
ON CONFLICT (code, version) DO NOTHING;

INSERT INTO stocks (code, name, sector)
VALUES ('BRIFO01', '브리포테크', '브리포'),
       ('BRIFO02', '브리포시스템', '브리포'),
       ('BRIFO03', '브리포랩', '브리포')
ON CONFLICT (code) DO NOTHING;

INSERT INTO daily_stock_prices (stock_id, trade_date, price, change_rate, fetched_at)
SELECT stock.id, DATE '2026-08-02', price, change_rate, fetched_at
FROM stocks stock
JOIN (
    VALUES ('BRIFO01', 121000.00, 0.80, TIMESTAMP '2026-08-02 11:10:00'),
           ('BRIFO01', 123500.00, 2.10, TIMESTAMP '2026-08-02 15:20:00'),
           ('BRIFO01', 125000.00, 3.45, TIMESTAMP '2026-08-02 20:40:00'),
           ('BRIFO02', 89500.00, 0.30, TIMESTAMP '2026-08-02 11:20:00'),
           ('BRIFO02', 88100.00, -0.70, TIMESTAMP '2026-08-02 15:30:00'),
           ('BRIFO02', 87200.00, -1.20, TIMESTAMP '2026-08-02 20:50:00'),
           ('BRIFO03', 53800.00, -0.40, TIMESTAMP '2026-08-02 11:30:00'),
           ('BRIFO03', 54100.00, 0.20, TIMESTAMP '2026-08-02 15:40:00'),
           ('BRIFO03', 54300.00, 0.00, TIMESTAMP '2026-08-02 21:00:00')
) AS price_data(code, price, change_rate, fetched_at) ON price_data.code = stock.code
WHERE NOT EXISTS (
    SELECT 1
    FROM daily_stock_prices existing
    WHERE existing.stock_id = stock.id
      AND existing.trade_date = DATE '2026-08-02'
      AND existing.fetched_at = price_data.fetched_at
);

INSERT INTO news (
    stock_id, source, source_url, title, summary, importance,
    dedup_key, processing_status, published_at, crawled_at
)
SELECT stock.id, 'TEST', data.source_url, data.title, data.summary, data.importance,
       data.dedup_key, 'PROCESSED', data.published_at, data.published_at + INTERVAL '10 minutes'
FROM stocks stock
JOIN (
    VALUES
        ('BRIFO01', 'https://example.test/news/brifo-tech-research-1', '브리포테크, 신규 기술 연구 계획 발표', '브리포테크가 신규 기술에 대한 연구 계획을 발표했습니다.', 0.92, 'dev-brifo-tech-20260802-1', TIMESTAMP '2026-08-02 08:20:00'),
        ('BRIFO01', 'https://example.test/news/brifo-tech-research-2', '브리포테크, 연구개발 투자 확대', '브리포테크가 연구개발 분야의 투자를 확대할 예정입니다.', 0.74, 'dev-brifo-tech-20260802-2', TIMESTAMP '2026-08-02 08:40:00'),
        ('BRIFO02', 'https://example.test/news/brifo-system-contract-1', '브리포시스템, 신규 공급 계약 체결', '브리포시스템이 신규 서비스 공급 계약을 체결했습니다.', 0.68, 'dev-brifo-system-20260802-1', TIMESTAMP '2026-08-02 09:10:00'),
        ('BRIFO02', 'https://example.test/news/brifo-system-contract-2', '브리포시스템, 추가 사업 수주', '브리포시스템이 기존 사업과 연계된 추가 사업을 수주했습니다.', 0.57, 'dev-brifo-system-20260802-2', TIMESTAMP '2026-08-02 09:30:00'),
        ('BRIFO03', 'https://example.test/news/brifo-lab-research-1', '브리포랩, 연구 결과 공개', '브리포랩이 진행 중인 연구의 중간 결과를 공개했습니다.', 0.35, 'dev-brifo-lab-20260802-1', TIMESTAMP '2026-08-02 10:00:00'),
        ('BRIFO03', 'https://example.test/news/brifo-lab-research-2', '브리포랩, 공동 연구 계약 체결', '브리포랩이 신규 공동 연구를 위한 계약을 체결했습니다.', 0.43, 'dev-brifo-lab-20260802-2', TIMESTAMP '2026-08-02 10:20:00')
) AS data(code, source_url, title, summary, importance, dedup_key, published_at)
    ON data.code = stock.code
ON CONFLICT (dedup_key) DO NOTHING;

INSERT INTO news_cards (news_id, headline, points, keywords, importance_badge, display_date, created_at)
SELECT news.id, data.headline, data.points::jsonb, data.keywords::jsonb,
       data.importance_badge, DATE '2026-08-02', data.created_at
FROM news
JOIN (
    VALUES
        ('dev-brifo-tech-20260802-1', '브리포테크, 신규 기술 연구 추진', '["브리포테크가 신규 기술 연구 계획을 발표했습니다.", "연구 성과에 따라 향후 주가가 영향을 받을 수 있습니다.", "회사는 연구 인력과 관련 투자를 단계적으로 확대할 예정입니다."]', '["신규 기술", "연구 계획", "연구 투자"]', 'HOT', TIMESTAMP '2026-08-02 11:00:00'),
        ('dev-brifo-tech-20260802-2', '브리포테크, 연구개발 투자 확대', '["브리포테크가 연구개발 예산을 확대하기로 했습니다.", "신규 과제 추진으로 관련 거래량이 늘어날 가능성이 있습니다.", "회사는 장기적인 기술 경쟁력 강화를 기대하고 있습니다."]', '["연구개발", "예산 확대", "신규 과제"]', 'MID', TIMESTAMP '2026-08-02 11:05:00'),
        ('dev-brifo-system-20260802-1', '브리포시스템, 신규 공급 계약', '["브리포시스템이 신규 서비스 공급 계약을 체결했습니다.", "계약 발표 당일 시가는 이전 거래일과 다른 흐름을 보였습니다.", "이번 계약으로 신규 매출이 발생할 것으로 예상됩니다."]', '["공급 계약", "신규 서비스", "매출"]', 'MID', TIMESTAMP '2026-08-02 15:00:00'),
        ('dev-brifo-system-20260802-2', '브리포시스템, 추가 사업 수주', '["브리포시스템이 기존 사업과 연계된 추가 사업을 수주했습니다.", "수주 소식이 반영되면서 종가는 장중 흐름과 차이를 보였습니다.", "구체적인 사업 일정은 추후 협의를 거쳐 확정될 예정입니다."]', '["사업 수주", "기존 사업", "사업 일정"]', 'LOW', TIMESTAMP '2026-08-02 15:05:00'),
        ('dev-brifo-lab-20260802-1', '브리포랩, 연구 중간 결과 공개', '["브리포랩이 진행 중인 연구의 중간 결과를 공개했습니다.", "긍정적인 결과 발표 이후 주가가 상승하는 흐름을 보였습니다.", "회사는 추가 연구를 통해 결과를 보완할 계획입니다."]', '["중간 결과", "연구 진행", "추가 연구"]', 'LOW', TIMESTAMP '2026-08-02 20:00:00'),
        ('dev-brifo-lab-20260802-2', '브리포랩, 공동 연구 계약', '["브리포랩이 신규 공동 연구 계약을 체결했습니다.", "계약 발표에도 불구하고 주가는 일시적으로 하락했습니다.", "양측은 후속 협의를 통해 세부 연구 범위를 확정할 예정입니다."]', '["공동 연구", "연구 계약", "후속 협의"]', 'MID', TIMESTAMP '2026-08-02 20:05:00')
) AS data(dedup_key, headline, points, keywords, importance_badge, created_at)
    ON data.dedup_key = news.dedup_key
ON CONFLICT (news_id) DO NOTHING;

INSERT INTO glossary_terms (term, definition, category)
VALUES ('주가', '주식 한 주의 가격입니다.', '기본'),
       ('거래량', '일정 기간 동안 거래된 주식의 수량입니다.', '기본'),
       ('시가', '거래가 시작될 때 처음 형성된 가격입니다.', '가격'),
       ('종가', '거래가 끝날 때 마지막으로 형성된 가격입니다.', '가격'),
       ('상승', '주식 가격이 이전보다 오른 상태입니다.', '등락'),
       ('하락', '주식 가격이 이전보다 내린 상태입니다.', '등락')
ON CONFLICT (term) DO NOTHING;

INSERT INTO news_card_terms (card_id, term_id, surface, display_order)
SELECT card.id, term.id, term.term, mapping.display_order
FROM (
    VALUES ('dev-brifo-tech-20260802-1', '주가', 0),
           ('dev-brifo-tech-20260802-2', '거래량', 0),
           ('dev-brifo-system-20260802-1', '시가', 0),
           ('dev-brifo-system-20260802-2', '종가', 0),
           ('dev-brifo-lab-20260802-1', '상승', 0),
           ('dev-brifo-lab-20260802-2', '하락', 0)
) AS mapping(dedup_key, term, display_order)
JOIN news ON news.dedup_key = mapping.dedup_key
JOIN news_cards card ON card.news_id = news.id
JOIN glossary_terms term ON term.term = mapping.term
ON CONFLICT DO NOTHING;
