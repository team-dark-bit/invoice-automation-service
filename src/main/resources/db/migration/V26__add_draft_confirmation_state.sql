ALTER TABLE conversation_contexts
    DROP CONSTRAINT conversation_contexts_state_check,
    DROP CONSTRAINT conversation_context_ready_data_check;

ALTER TABLE conversation_contexts
    ADD CONSTRAINT conversation_contexts_state_check CHECK (
        state IN ('EMPTY', 'COLLECTING_DATA', 'PROCESSING_MEDIA', 'NEEDS_REVIEW',
            'READY_TO_CREATE', 'AWAITING_DRAFT_CONFIRMATION', 'DRAFT_CREATED')
    ),
    ADD CONSTRAINT conversation_context_ready_data_check CHECK (
        state NOT IN ('READY_TO_CREATE', 'AWAITING_DRAFT_CONFIRMATION')
        OR (document_type IS NOT NULL AND recipient_document_type IS NOT NULL
            AND recipient_document_number IS NOT NULL)
    );
