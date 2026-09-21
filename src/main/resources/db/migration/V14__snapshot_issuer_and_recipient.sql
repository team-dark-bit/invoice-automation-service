-- Preserve the exact issuer and recipient data used when a document is emitted.
ALTER TABLE customers
    ADD COLUMN address VARCHAR(500),
    ADD COLUMN email VARCHAR(320);

ALTER TABLE electronic_documents
    ADD COLUMN issuer_tax_id VARCHAR(32),
    ADD COLUMN issuer_legal_name VARCHAR(255),
    ADD COLUMN issuer_trade_name VARCHAR(255),
    ADD COLUMN issuer_taxpayer_type VARCHAR(40),
    ADD COLUMN issuer_fiscal_address VARCHAR(500),
    ADD COLUMN issuer_ubigeo VARCHAR(6),
    ADD COLUMN issuer_department VARCHAR(100),
    ADD COLUMN issuer_province VARCHAR(100),
    ADD COLUMN issuer_district VARCHAR(100),
    ADD COLUMN issuer_country_code VARCHAR(2),
    ADD COLUMN recipient_name VARCHAR(255),
    ADD COLUMN recipient_address VARCHAR(500),
    ADD COLUMN recipient_email VARCHAR(320);

UPDATE electronic_documents document
SET issuer_tax_id = company.tax_id,
    issuer_legal_name = company.legal_name,
    issuer_trade_name = company.trade_name,
    issuer_taxpayer_type = COALESCE(profile.taxpayer_type, 'LEGAL_ENTITY'),
    issuer_fiscal_address = COALESCE(profile.fiscal_address, company.address, 'NOT PROVIDED'),
    issuer_ubigeo = COALESCE(profile.ubigeo, '000000'),
    issuer_department = profile.department,
    issuer_province = profile.province,
    issuer_district = profile.district,
    issuer_country_code = COALESCE(profile.country_code, 'PE'),
    recipient_name = COALESCE(customer.company_name, customer.full_name,
        document.recipient_document_number),
    recipient_address = customer.address,
    recipient_email = customer.email
FROM companies company
LEFT JOIN issuer_tax_profiles profile ON profile.company_id = company.id
JOIN customers customer ON TRUE
WHERE company.id = document.company_id
  AND customer.id = document.customer_id;

ALTER TABLE electronic_documents
    ALTER COLUMN issuer_tax_id SET NOT NULL,
    ALTER COLUMN issuer_legal_name SET NOT NULL,
    ALTER COLUMN issuer_taxpayer_type SET NOT NULL,
    ALTER COLUMN issuer_fiscal_address SET NOT NULL,
    ALTER COLUMN issuer_ubigeo SET NOT NULL,
    ALTER COLUMN issuer_country_code SET NOT NULL,
    ALTER COLUMN recipient_name SET NOT NULL;
