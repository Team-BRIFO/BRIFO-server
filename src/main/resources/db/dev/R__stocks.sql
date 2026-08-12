INSERT INTO stocks (code, name, sector, logo_url)
VALUES
    ('005930', '삼성전자', 'IT', 'https://static.toss.im/png-icons/securities/icn-sec-fill-005930.png'),
    ('000660', 'SK하이닉스', 'IT', 'https://static.toss.im/png-icons/securities/icn-sec-fill-000660.png'),
    ('373220', 'LG에너지솔루션', 'IT/에너지', 'https://static.toss.im/png-icons/securities/icn-sec-fill-373220.png'),
    ('207940', '삼성바이오로직스', '헬스케어', 'https://static.toss.im/png-icons/securities/icn-sec-fill-207940.png'),
    ('005380', '현대차', '자동차', 'https://static.toss.im/png-icons/securities/icn-sec-fill-005380.png'),
    ('000270', '기아', '자동차', 'https://static.toss.im/png-icons/securities/icn-sec-fill-000270.png'),
    ('068270', '셀트리온', '헬스케어', 'https://static.toss.im/png-icons/securities/icn-sec-fill-068270.png'),
    ('005490', 'POSCO홀딩스', '철강/소재', 'https://static.toss.im/png-icons/securities/icn-sec-fill-005490.png'),
    ('035420', 'NAVER', 'IT/플랫폼', 'https://static.toss.im/png-icons/securities/icn-sec-fill-035420.png'),
    ('006400', '삼성SDI', 'IT/에너지', 'https://static.toss.im/png-icons/securities/icn-sec-fill-006400.png'),
    ('051910', 'LG화학', '화학', 'https://static.toss.im/png-icons/securities/icn-sec-fill-051910.png'),
    ('028260', '삼성물산', '산업재', 'https://static.toss.im/png-icons/securities/icn-sec-fill-028260.png'),
    ('035720', '카카오', 'IT/플랫폼', 'https://static.toss.im/png-icons/securities/icn-sec-fill-035720.png'),
    ('105560', 'KB금융', '금융', 'https://static.toss.im/png-icons/securities/icn-sec-fill-105560.png'),
    ('012330', '현대모비스', '자동차', 'https://static.toss.im/png-icons/securities/icn-sec-fill-012330.png'),
    ('055550', '신한지주', '금융', 'https://static.toss.im/png-icons/securities/icn-sec-fill-055550.png'),
    ('066570', 'LG전자', 'IT', 'https://static.toss.im/png-icons/securities/icn-sec-fill-066570.png'),
    ('032830', '삼성생명', '금융', 'https://static.toss.im/png-icons/securities/icn-sec-fill-032830.png'),
    ('003550', 'LG', '지주사', 'https://static.toss.im/png-icons/securities/icn-sec-fill-003550.png'),
    ('086790', '하나금융지주', '금융', 'https://static.toss.im/png-icons/securities/icn-sec-fill-086790.png'),
    ('015760', '한국전력', '유틸리티', 'https://static.toss.im/png-icons/securities/icn-sec-fill-015760.png'),
    ('033780', 'KT&G', '필수소비재', 'https://static.toss.im/png-icons/securities/icn-sec-fill-033780.png'),
    ('000810', '삼성화재', '금융', 'https://static.toss.im/png-icons/securities/icn-sec-fill-000810.png'),
    ('034730', 'SK', '지주사', 'https://static.toss.im/png-icons/securities/icn-sec-fill-034730.png'),
    ('323410', '카카오뱅크', '금융', 'https://static.toss.im/png-icons/securities/icn-sec-fill-323410.png'),
    ('316140', '우리금융지주', '금융', 'https://static.toss.im/png-icons/securities/icn-sec-fill-316140.png'),
    ('018260', '삼성SDS', 'IT', 'https://static.toss.im/png-icons/securities/icn-sec-fill-018260.png'),
    ('011200', 'HMM', '산업재', 'https://static.toss.im/png-icons/securities/icn-sec-fill-011200.png'),
    ('010130', '고려아연', '철강/소재', 'https://static.toss.im/png-icons/securities/icn-sec-fill-010130.png'),
    ('009150', '삼성전기', 'IT', 'https://static.toss.im/png-icons/securities/icn-sec-fill-009150.png'),
    ('004020', '현대제철', '철강/소재', 'https://static.toss.im/png-icons/securities/icn-sec-fill-004020.png'),
    ('010950', 'S-Oil', '에너지', 'https://static.toss.im/png-icons/securities/icn-sec-fill-010950.png'),
    ('090430', '아모레퍼시픽', '소비재', 'https://static.toss.im/png-icons/securities/icn-sec-fill-090430.png'),
    ('036570', '엔씨소프트', 'IT/게임', 'https://static.toss.im/png-icons/securities/icn-sec-fill-036570.png'),
    ('003490', '대한항공', '산업재', 'https://static.toss.im/png-icons/securities/icn-sec-fill-003490.png'),
    ('096770', 'SK이노베이션', '에너지', 'https://static.toss.im/png-icons/securities/icn-sec-fill-096770.png'),
    ('011170', '롯데케미칼', '화학', 'https://static.toss.im/png-icons/securities/icn-sec-fill-011170.png')
ON CONFLICT (code) DO UPDATE
    SET name = EXCLUDED.name,
        sector = EXCLUDED.sector,
        logo_url = EXCLUDED.logo_url;

INSERT INTO daily_stock_prices (stock_id, trade_date, price, change_rate, fetched_at, is_closing)
SELECT stock.id, DATE '1970-01-01', 0.00, 0.00, TIMESTAMP '1970-01-01 00:00:00', FALSE
FROM stocks stock
WHERE stock.code IN (
    '005930', '000660', '373220', '207940', '005380', '000270', '068270', '005490', '035420', '006400',
    '051910', '028260', '035720', '105560', '012330', '055550', '066570', '032830', '003550', '086790',
    '015760', '033780', '000810', '034730', '323410', '316140', '018260', '011200', '010130', '009150',
    '004020', '010950', '090430', '036570', '003490', '096770', '011170'
)
  AND NOT EXISTS (
    SELECT 1
    FROM daily_stock_prices price
    WHERE price.stock_id = stock.id
      AND price.trade_date = DATE '1970-01-01'
);
