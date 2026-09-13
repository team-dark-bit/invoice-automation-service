ALTER TABLE invoice_draft
    ADD COLUMN provider_reference VARCHAR(255),
    ADD COLUMN issued_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE invoice_draft
    ADD CONSTRAINT chk_invoice_draft_issuance
        CHECK (
            (status <> 'ISSUED' AND provider_reference IS NULL AND issued_at IS NULL)
            OR
            (status = 'ISSUED' AND provider_reference IS NOT NULL AND issued_at IS NOT NULL)
        );
