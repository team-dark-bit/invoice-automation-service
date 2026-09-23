ALTER TABLE document_series
    DROP CONSTRAINT IF EXISTS document_series_type_check;
ALTER TABLE document_series
    ADD CONSTRAINT document_series_type_check
        CHECK (document_type IN ('INVOICE', 'SALES_RECEIPT', 'CREDIT_NOTE', 'DEBIT_NOTE'));

ALTER TABLE electronic_documents
    ALTER COLUMN draft_id DROP NOT NULL,
    ADD COLUMN related_document_id UUID,
    ADD COLUMN related_document_type VARCHAR(30),
    ADD COLUMN related_series VARCHAR(4),
    ADD COLUMN related_correlative BIGINT,
    ADD COLUMN note_reason_code VARCHAR(2),
    ADD COLUMN note_reason VARCHAR(500),
    ADD CONSTRAINT electronic_documents_related_fkey
        FOREIGN KEY (related_document_id) REFERENCES electronic_documents(id),
    ADD CONSTRAINT electronic_documents_note_data_check CHECK (
        (document_type IN ('INVOICE', 'SALES_RECEIPT')
            AND draft_id IS NOT NULL AND related_document_id IS NULL)
        OR
        (document_type IN ('CREDIT_NOTE', 'DEBIT_NOTE')
            AND draft_id IS NULL AND related_document_id IS NOT NULL
            AND related_document_type IN ('INVOICE', 'SALES_RECEIPT')
            AND related_series IS NOT NULL AND related_correlative IS NOT NULL
            AND note_reason_code IS NOT NULL AND note_reason IS NOT NULL)
    );

CREATE UNIQUE INDEX electronic_documents_note_idempotency
    ON electronic_documents(related_document_id, document_type, note_reason_code);
