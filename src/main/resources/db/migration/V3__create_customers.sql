CREATE TABLE IF NOT EXISTS customers (
    id VARCHAR(36) PRIMARY KEY,
    full_name VARCHAR(255),
    company_name VARCHAR(255),
    document_type VARCHAR(32),
    document_number VARCHAR(32),
    phone_number VARCHAR(32),
    active BOOLEAN NOT NULL DEFAULT TRUE
);
