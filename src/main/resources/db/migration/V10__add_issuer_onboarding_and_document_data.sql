-- V10: issuer tax onboarding, automatic recipient lookup support and document type.

CREATE TABLE issuer_tax_profiles (
    company_id VARCHAR(36) PRIMARY KEY,
    taxpayer_type VARCHAR(40) NOT NULL,
    fiscal_address VARCHAR(500) NOT NULL,
    ubigeo VARCHAR(6) NOT NULL,
    department VARCHAR(100) NOT NULL,
    province VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    country_code VARCHAR(2) NOT NULL DEFAULT 'PE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT issuer_tax_profiles_company_fkey
        FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT issuer_tax_profiles_ubigeo_check CHECK (ubigeo ~ '^[0-9]{6}$'),
    CONSTRAINT issuer_tax_profiles_country_check CHECK (country_code ~ '^[A-Z]{2}$')
);

CREATE INDEX idx_customers_company_document
    ON customers(company_id, document_type, document_number);

ALTER TABLE invoice_draft
    ADD COLUMN document_type VARCHAR(30),
    ADD COLUMN recipient_document_type VARCHAR(10),
    ADD COLUMN recipient_document_number VARCHAR(32);

UPDATE invoice_draft draft
SET document_type = 'SALES_RECEIPT',
    recipient_document_type = CASE
        WHEN customer.document_type = 'RUC' THEN 'RUC'
        ELSE 'DNI'
    END,
    recipient_document_number = CASE
        WHEN customer.document_type = 'RUC'
             AND customer.document_number ~ '^[0-9]{11}$'
            THEN customer.document_number
        WHEN customer.document_type = 'DNI'
             AND customer.document_number ~ '^[0-9]{8}$'
            THEN customer.document_number
        ELSE '00000000'
    END
FROM customers customer
WHERE customer.id = draft.customer_id;

UPDATE invoice_draft
SET document_type = COALESCE(document_type, 'SALES_RECEIPT'),
    recipient_document_type = COALESCE(recipient_document_type, 'DNI'),
    recipient_document_number = COALESCE(recipient_document_number, '00000000');

ALTER TABLE invoice_draft
    ALTER COLUMN document_type SET NOT NULL,
    ALTER COLUMN recipient_document_type SET NOT NULL,
    ALTER COLUMN recipient_document_number SET NOT NULL,
    ADD CONSTRAINT invoice_draft_document_type_check
        CHECK (document_type IN ('INVOICE', 'SALES_RECEIPT')),
    ADD CONSTRAINT invoice_draft_recipient_document_type_check
        CHECK (recipient_document_type IN ('DNI', 'RUC'));
