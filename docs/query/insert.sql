-- queries de inserción para permisos, roles y usuarios
INSERT INTO permissions (id, name, description, category) VALUES
    ('504b6d73-d4d1-4128-b89b-32b8e3343239','TOKEN', 'Usar token para consumo de apis', 'CONSUME_TOKEN');

INSERT INTO roles (id,"name",description) VALUES
    ('66deb0c8-5a55-493b-a359-d915899ceb66','ROLE_ADMIN','Propietario del proyecto');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_ADMIN' AND p.name IN (
    'TOKEN'
    );

INSERT INTO public.users
(id, full_name, username, email, "password", enabled, created_at, last_login)
VALUES('bb943bde-99df-5229-b50e-3647631cdc43', 'Harold Quispe', 'haroldqc', 'haroldqc@gmail.com', '$2a$10$UtwHOVYjEP5NyYcoBg3pdevS3261bNot0sg5DneQ4ycvcgPSYU46a', true, '2026-03-22 22:53:44.000', null);
