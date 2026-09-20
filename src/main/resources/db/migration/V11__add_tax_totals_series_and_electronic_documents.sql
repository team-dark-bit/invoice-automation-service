-- V11: tax calculation fields, transactional series and immutable electronic documents.

ALTER TABLE invoice_items
    ADD COLUMN unit_code VARCHAR(10) NOT NULL DEFAULT 'NIU',
    ADD COLUMN discount NUMERIC(20, 2) NOT NULL DEFAULT 0,
    ADD COLUMN tax_affectation VARCHAR(20) NOT NULL DEFAULT 'UNAFFECTED',
    ADD COLUMN tax_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    ADD COLUMN gross_amount NUMERIC(20, 2),
    ADD COLUMN taxable_amount NUMERIC(20, 2) NOT NULL DEFAULT 0,
    ADD COLUMN tax_amount NUMERIC(20, 2) NOT NULL DEFAULT 0;

UPDATE invoice_items SET gross_amount = ROUND(line_total, 2);
ALTER TABLE invoice_items ALTER COLUMN gross_amount SET NOT NULL;

ALTER TABLE invoice_draft
    ADD COLUMN discount_total NUMERIC(20, 2) NOT NULL DEFAULT 0,
    ADD COLUMN taxable_total NUMERIC(20, 2) NOT NULL DEFAULT 0,
    ADD COLUMN tax_total NUMERIC(20, 2) NOT NULL DEFAULT 0;

CREATE TABLE document_series (
    id VARCHAR(36) PRIMARY KEY,
    company_id VARCHAR(36) NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    series VARCHAR(4) NOT NULL,
    current_correlative BIGINT NOT NULL DEFAULT 0 CHECK (current_correlative >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT document_series_company_fkey FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT document_series_type_check CHECK (document_type IN ('INVOICE', 'SALES_RECEIPT')),
    CONSTRAINT document_series_format_check CHECK (series ~ '^[FB][A-Z0-9]{3}$'),
    CONSTRAINT document_series_unique UNIQUE (company_id, document_type, series)
);

CREATE INDEX idx_document_series_lookup
    ON document_series(company_id, document_type, active);

CREATE TABLE electronic_documents (
    id UUID PRIMARY KEY,
    draft_id UUID NOT NULL UNIQUE,
    company_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    series VARCHAR(4) NOT NULL,
    correlative BIGINT NOT NULL CHECK (correlative > 0),
    full_number VARCHAR(20) NOT NULL,
    recipient_document_type VARCHAR(10) NOT NULL,
    recipient_document_number VARCHAR(32) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    subtotal NUMERIC(20, 2) NOT NULL,
    discount_total NUMERIC(20, 2) NOT NULL,
    taxable_total NUMERIC(20, 2) NOT NULL,
    tax_total NUMERIC(20, 2) NOT NULL,
    total NUMERIC(20, 2) NOT NULL,
    provider_reference VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT electronic_documents_draft_fkey FOREIGN KEY (draft_id) REFERENCES invoice_draft(id),
    CONSTRAINT electronic_documents_company_fkey FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT electronic_documents_customer_fkey FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT electronic_documents_number_unique
        UNIQUE (company_id, document_type, series, correlative)
);

CREATE INDEX idx_electronic_documents_company_issued
    ON electronic_documents(company_id, issued_at);

CREATE TABLE electronic_document_items (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    position INTEGER NOT NULL CHECK (position >= 0),
    description VARCHAR(500) NOT NULL,
    unit_code VARCHAR(10) NOT NULL,
    quantity NUMERIC(16, 4) NOT NULL,
    unit_price NUMERIC(20, 2) NOT NULL,
    discount NUMERIC(20, 2) NOT NULL,
    tax_affectation VARCHAR(20) NOT NULL,
    tax_rate NUMERIC(5, 2) NOT NULL,
    gross_amount NUMERIC(20, 2) NOT NULL,
    taxable_amount NUMERIC(20, 2) NOT NULL,
    tax_amount NUMERIC(20, 2) NOT NULL,
    line_total NUMERIC(20, 2) NOT NULL,
    CONSTRAINT electronic_document_items_document_fkey
        FOREIGN KEY (document_id) REFERENCES electronic_documents(id) ON DELETE CASCADE,
    CONSTRAINT electronic_document_items_position_unique UNIQUE (document_id, position)
);
