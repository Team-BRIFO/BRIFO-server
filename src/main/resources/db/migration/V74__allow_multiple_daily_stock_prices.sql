ALTER TABLE daily_stock_prices
    DROP CONSTRAINT daily_stock_prices_stock_id_trade_date_key;

CREATE UNIQUE INDEX idx_daily_stock_prices_stock_fetched_unique
    ON daily_stock_prices (stock_id, trade_date, fetched_at);
