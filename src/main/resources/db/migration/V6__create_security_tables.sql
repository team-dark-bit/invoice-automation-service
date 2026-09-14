-- V6: create security tables for users/roles/permissions

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    "password" VARCHAR(255) NOT NULL,
    enabled BOOLEAN NULL,
    created_at TIMESTAMP WITH TIME ZONE NULL,
    last_login TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT users_email_key UNIQUE (email),
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_username_key UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS roles (
    id VARCHAR(36) NOT NULL,
    "name" VARCHAR(50) NOT NULL,
    description VARCHAR(150) NULL,
    CONSTRAINT roles_name_key UNIQUE ("name"),
    CONSTRAINT roles_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(36) NOT NULL,
    CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id),
    CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS permissions (
    id VARCHAR(36) NOT NULL,
    "name" VARCHAR(30) NOT NULL,
    description VARCHAR(255) NULL,
    category VARCHAR(50) NULL,
    CONSTRAINT permissions_name_key UNIQUE ("name"),
    CONSTRAINT permissions_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id),
    CONSTRAINT role_permissions_permission_id_fkey FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    CONSTRAINT role_permissions_role_id_fkey FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);
