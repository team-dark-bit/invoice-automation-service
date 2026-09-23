ALTER TABLE electronic_documents
    DROP CONSTRAINT IF EXISTS electronic_documents_status_check,
    DROP CONSTRAINT IF EXISTS electronic_documents_delivery_data_check;

ALTER TABLE electronic_documents
    ADD CONSTRAINT electronic_documents_status_check
        CHECK (status IN ('PENDING_SEND', 'SENDING', 'SENT', 'ACCEPTED', 'REJECTED', 'ERROR')),
    ADD CONSTRAINT electronic_documents_delivery_data_check
        CHECK (
            (status = 'PENDING_SEND' AND provider_reference IS NULL AND submitted_at IS NULL)
            OR
            (status = 'SENDING' AND provider_reference IS NULL AND submitted_at IS NOT NULL)
            OR
            (status = 'ERROR')
            OR
            (status IN ('SENT', 'ACCEPTED', 'REJECTED')
                AND provider_reference IS NOT NULL AND submitted_at IS NOT NULL)
        );
