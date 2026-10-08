ALTER TABLE users ADD COLUMN default_company_id VARCHAR(36);

ALTER TABLE users
    ADD CONSTRAINT users_default_company_fkey
        FOREIGN KEY (default_company_id) REFERENCES companies(id) ON DELETE SET NULL;

UPDATE users app_user
SET default_company_id = (
    SELECT membership.company_id
    FROM user_companies membership
    WHERE membership.user_id = app_user.id
      AND membership.active = TRUE
    ORDER BY membership.created_at, membership.company_id
    LIMIT 1
)
WHERE app_user.default_company_id IS NULL;

CREATE INDEX idx_users_default_company_id ON users(default_company_id);
