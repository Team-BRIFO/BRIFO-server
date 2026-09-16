ALTER TABLE stocks
    ADD COLUMN fame_rank INTEGER;

CREATE INDEX idx_stocks_fame_rank ON stocks (fame_rank);
