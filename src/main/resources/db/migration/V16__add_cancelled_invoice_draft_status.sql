ALTER TABLE invoice_draft
    DROP CONSTRAINT IF EXISTS invoice_draft_status_check;

ALTER TABLE invoice_draft
    ADD CONSTRAINT invoice_draft_status_check
        CHECK (status IN ('DRAFT', 'APPROVED', 'ISSUED', 'CANCELLED'));
