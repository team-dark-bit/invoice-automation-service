-- Provider delivery lifecycle. Fiscal document data remains immutable; only
-- these transport/acceptance fields can evolve after submission.
ALTER TABLE electronic_documents
    RENAME COLUMN issued_at TO submitted_at;

ALTER TABLE electronic_documents
    ALTER COLUMN provider_reference DROP NOT NULL,
    ALTER COLUMN submitted_at DROP NOT NULL,
    ADD COLUMN status VARCHAR(30),
    ADD COLUMN responded_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN provider_response_code VARCHAR(100),
    ADD COLUMN provider_response_message VARCHAR(1000);

UPDATE electronic_documents
SET status = 'ACCEPTED', responded_at = submitted_at
WHERE status IS NULL;

ALTER TABLE electronic_documents
    ALTER COLUMN status SET NOT NULL,
    ADD CONSTRAINT electronic_documents_status_check
        CHECK (status IN ('PENDING_SEND', 'SENT', 'ACCEPTED', 'REJECTED', 'ERROR')),
    ADD CONSTRAINT electronic_documents_delivery_data_check
        CHECK (
            (status = 'PENDING_SEND' AND provider_reference IS NULL AND submitted_at IS NULL)
            OR
            (status = 'ERROR')
            OR
            (status IN ('SENT', 'ACCEPTED', 'REJECTED')
                AND provider_reference IS NOT NULL AND submitted_at IS NOT NULL)
        );

DROP INDEX IF EXISTS idx_electronic_documents_company_issued;
CREATE INDEX idx_electronic_documents_company_status
    ON electronic_documents(company_id, status, submitted_at);
