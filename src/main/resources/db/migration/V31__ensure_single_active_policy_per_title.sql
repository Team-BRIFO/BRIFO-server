CREATE UNIQUE INDEX idx_policies_active_title_unique
    ON policies (title)
    WHERE is_active = TRUE;
