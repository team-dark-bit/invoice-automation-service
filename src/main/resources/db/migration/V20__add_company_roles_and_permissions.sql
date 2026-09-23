-- Roles are scoped to a company. Global roles remain authentication authorities only.
ALTER TABLE user_companies ADD COLUMN role VARCHAR(20);
ALTER TABLE user_companies ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE user_companies SET role = 'OWNER' WHERE role IS NULL;
ALTER TABLE user_companies ALTER COLUMN role SET NOT NULL;
ALTER TABLE user_companies ADD CONSTRAINT user_companies_role_check
    CHECK (role IN ('OWNER', 'ADMIN', 'BILLING', 'VIEWER'));

INSERT INTO permissions (id, "name", description, category) VALUES
('2f4f6da1-3d25-4f84-a001-000000000001', 'TOKEN', 'Use authenticated API token', 'AUTH')
ON CONFLICT ("name") DO NOTHING;

INSERT INTO roles (id, "name", description) VALUES
('2f4f6da1-3d25-4f84-a002-000000000001', 'ROLE_USER', 'Authenticated platform user')
ON CONFLICT ("name") DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id FROM roles role CROSS JOIN permissions permission
WHERE role."name" = 'ROLE_USER' AND permission."name" = 'TOKEN'
ON CONFLICT DO NOTHING;
