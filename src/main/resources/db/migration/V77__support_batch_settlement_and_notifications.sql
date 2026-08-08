ALTER TABLE daily_stock_prices
    ADD COLUMN is_closing BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX idx_daily_stock_prices_closing_unique
    ON daily_stock_prices (stock_id, trade_date)
    WHERE is_closing = TRUE;

ALTER TABLE notifications
    ADD COLUMN event_date DATE;

UPDATE notifications notification
SET event_date = notification.created_at::DATE
FROM notification_types type
WHERE notification.notification_type_id = type.id
  AND type.code = 'NEWS_CARD_ARRIVED';

CREATE UNIQUE INDEX idx_notifications_dated_event_unique
    ON notifications (user_id, notification_type_id, target_type, target_public_id, event_date)
    WHERE event_date IS NOT NULL;
