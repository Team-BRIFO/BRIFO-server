ALTER TABLE policies
    ADD COLUMN code VARCHAR(50) NOT NULL,
    DROP CONSTRAINT policies_title_version_key,
    ADD CONSTRAINT policies_code_version_unique UNIQUE (code, version);

CREATE UNIQUE INDEX idx_policies_active_code_unique
    ON policies (code)
    WHERE is_active = TRUE;
