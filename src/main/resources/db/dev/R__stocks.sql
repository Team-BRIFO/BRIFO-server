INSERT INTO stocks (code, name, sector)
VALUES ('005930', '삼성전자', '반도체'),
       ('000660', 'SK하이닉스', '반도체'),
       ('035420', 'NAVER', '인터넷'),
       ('035720', '카카오', '인터넷'),
       ('005380', '현대차', '자동차'),
       ('000270', '기아', '자동차'),
       ('373220', 'LG에너지솔루션', '2차전지'),
       ('207940', '삼성바이오로직스', '바이오'),
       ('005490', 'POSCO홀딩스', '철강'),
       ('105560', 'KB금융', '금융')
ON CONFLICT (code) DO NOTHING;

INSERT INTO daily_stock_prices (stock_id, trade_date, price, change_rate, fetched_at, is_closing)
SELECT stock.id, DATE '1970-01-01', 0.00, 0.00, TIMESTAMP '1970-01-01 00:00:00', FALSE
FROM stocks stock
WHERE stock.code IN (
    '005930', '000660', '035420', '035720', '005380',
    '000270', '373220', '207940', '005490', '105560'
)
  AND NOT EXISTS (
    SELECT 1
    FROM daily_stock_prices price
    WHERE price.stock_id = stock.id
      AND price.trade_date = DATE '1970-01-01'
);
