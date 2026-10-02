ALTER TABLE conversation_contexts
    DROP CONSTRAINT conversation_contexts_state_check,
    DROP CONSTRAINT conversation_context_state_check;

UPDATE conversation_contexts context
SET state = CASE
    WHEN EXISTS (
        SELECT 1 FROM conversation_context_items item
        WHERE item.conversation_id = context.conversation_id
    ) THEN 'READY_TO_CREATE'
    ELSE 'COLLECTING_DATA'
END
WHERE state = 'COLLECTING_ITEMS';

ALTER TABLE conversation_contexts
    ADD COLUMN last_interpretation_json TEXT,
    ADD COLUMN review_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT conversation_contexts_state_check CHECK (
        state IN ('EMPTY', 'COLLECTING_DATA', 'PROCESSING_MEDIA', 'NEEDS_REVIEW',
            'READY_TO_CREATE', 'DRAFT_CREATED')
    ),
    ADD CONSTRAINT conversation_context_draft_state_check CHECK (
        (state = 'DRAFT_CREATED' AND invoice_draft_id IS NOT NULL)
        OR (state <> 'DRAFT_CREATED' AND invoice_draft_id IS NULL)
    ),
    ADD CONSTRAINT conversation_context_review_state_check CHECK (
        review_required = (state = 'NEEDS_REVIEW')
        AND (review_required = FALSE OR last_interpretation_json IS NOT NULL)
    ),
    ADD CONSTRAINT conversation_context_ready_data_check CHECK (
        state <> 'READY_TO_CREATE'
        OR (document_type IS NOT NULL AND recipient_document_type IS NOT NULL
            AND recipient_document_number IS NOT NULL)
    );
