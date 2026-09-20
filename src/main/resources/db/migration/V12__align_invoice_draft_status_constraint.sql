-- Align databases created from the original bootstrap SQL with the current
-- draft lifecycle. Some existing installations restricted the status to
-- DRAFT and ISSUED before APPROVED was introduced.
ALTER TABLE invoice_draft
    DROP CONSTRAINT IF EXISTS invoice_draft_status_check;

ALTER TABLE invoice_draft
    ADD CONSTRAINT invoice_draft_status_check
        CHECK (status IN ('DRAFT', 'APPROVED', 'ISSUED'));
