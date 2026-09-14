-- V7: seed base security data required for local and Docker environments.
-- Every statement is idempotent so existing installations keep their current data.

INSERT INTO permissions (id, "name", description, category)
VALUES (
    '504b6d73-d4d1-4128-b89b-32b8e3343239',
    'TOKEN',
    'Usar token para consumo de apis',
    'CONSUME_TOKEN'
)
ON CONFLICT DO NOTHING;

INSERT INTO roles (id, "name", description)
VALUES (
    '66deb0c8-5a55-493b-a359-d915899ceb66',
    'ROLE_ADMIN',
    'Propietario del proyecto'
)
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission."name" = 'TOKEN'
WHERE role."name" = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

-- Local test credentials: haroldqc / password
INSERT INTO users (
    id,
    full_name,
    username,
    email,
    "password",
    enabled,
    created_at,
    last_login
)
VALUES (
    'bb943bde-99df-5229-b50e-3647631cdc43',
    'Harold Quispe',
    'haroldqc',
    'haroldqc@gmail.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    TRUE,
    CURRENT_TIMESTAMP,
    NULL
)
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT app_user.id, role.id
FROM users app_user
JOIN roles role ON role."name" = 'ROLE_ADMIN'
WHERE app_user.username = 'haroldqc'
ON CONFLICT DO NOTHING;
