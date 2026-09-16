-- V9: associate authenticated users with companies and scope customers by company.

CREATE TABLE user_companies (
    user_id VARCHAR(36) NOT NULL,
    company_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_companies_pkey PRIMARY KEY (user_id, company_id),
    CONSTRAINT user_companies_user_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT user_companies_company_fkey FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_companies_company_id ON user_companies(company_id);

-- Preserve access to companies created before tenancy was introduced.
INSERT INTO user_companies (user_id, company_id)
SELECT app_user.id, company.id
FROM users app_user
CROSS JOIN companies company
WHERE app_user.username = 'haroldqc'
ON CONFLICT DO NOTHING;

ALTER TABLE customers ADD COLUMN company_id VARCHAR(36);

-- Prefer the company already used by an existing draft.
UPDATE customers customer
SET company_id = (
    SELECT MIN(draft.company_id)
    FROM invoice_draft draft
    WHERE draft.customer_id = customer.id
)
WHERE EXISTS (
    SELECT 1
    FROM invoice_draft draft
    WHERE draft.customer_id = customer.id
);

-- Legacy customers without drafts are assigned deterministically to the first existing company.
UPDATE customers
SET company_id = (SELECT MIN(id) FROM companies)
WHERE company_id IS NULL
  AND EXISTS (SELECT 1 FROM companies);

ALTER TABLE customers
    ADD CONSTRAINT customers_company_fkey
        FOREIGN KEY (company_id) REFERENCES companies(id);

CREATE INDEX idx_customers_company_active ON customers(company_id, active);

-- Fresh installations and consistent legacy databases can enforce the invariant immediately.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM customers WHERE company_id IS NULL) THEN
        ALTER TABLE customers ALTER COLUMN company_id SET NOT NULL;
    END IF;
END $$;
