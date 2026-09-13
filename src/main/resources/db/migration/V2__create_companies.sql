CREATE TABLE companies (
    id VARCHAR(36) PRIMARY KEY,
    legal_name VARCHAR(255) NOT NULL,
    trade_name VARCHAR(255),
    tax_id VARCHAR(32) NOT NULL UNIQUE,
    address VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE
);
