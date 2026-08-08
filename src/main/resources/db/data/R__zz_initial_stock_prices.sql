INSERT INTO daily_stock_prices (stock_id, trade_date, price, change_rate, fetched_at, is_closing)
SELECT stock.id, DATE '1970-01-01', 0.00, 0.00, TIMESTAMP '1970-01-01 00:00:00', FALSE
FROM stocks stock
WHERE stock.code IN ('BRIFO01', 'BRIFO02', 'BRIFO03')
  AND NOT EXISTS (
      SELECT 1
      FROM daily_stock_prices price
      WHERE price.stock_id = stock.id
        AND price.trade_date = DATE '1970-01-01'
  );
