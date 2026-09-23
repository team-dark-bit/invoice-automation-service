CREATE TABLE conversation_contexts (
    conversation_id UUID PRIMARY KEY REFERENCES conversations(id) ON DELETE CASCADE,
    state VARCHAR(30) NOT NULL CHECK (state IN ('EMPTY', 'COLLECTING_ITEMS', 'DRAFT_CREATED')),
    document_type VARCHAR(30),
    recipient_document_type VARCHAR(20),
    recipient_document_number VARCHAR(20),
    currency VARCHAR(3) NOT NULL DEFAULT 'PEN',
    invoice_draft_id UUID REFERENCES invoice_draft(id),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT conversation_context_state_check CHECK (
        (state = 'EMPTY' AND document_type IS NULL AND recipient_document_type IS NULL
            AND recipient_document_number IS NULL AND invoice_draft_id IS NULL)
        OR (state = 'COLLECTING_ITEMS' AND document_type IS NOT NULL
            AND recipient_document_type IS NOT NULL AND recipient_document_number IS NOT NULL
            AND invoice_draft_id IS NULL)
        OR (state = 'DRAFT_CREATED' AND document_type IS NOT NULL
            AND recipient_document_type IS NOT NULL AND recipient_document_number IS NOT NULL
            AND invoice_draft_id IS NOT NULL)
    )
);

CREATE TABLE conversation_context_items (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversation_contexts(conversation_id) ON DELETE CASCADE,
    position INTEGER NOT NULL CHECK (position >= 0),
    description VARCHAR(500) NOT NULL,
    unit_code VARCHAR(10) NOT NULL CHECK (unit_code IN ('NIU', 'ZZ', 'KGM', 'LTR')),
    quantity NUMERIC(16, 4) NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(16, 2) NOT NULL CHECK (unit_price >= 0),
    discount NUMERIC(16, 2) NOT NULL DEFAULT 0 CHECK (discount >= 0),
    tax_affectation VARCHAR(20) NOT NULL CHECK (tax_affectation IN ('TAXED', 'EXEMPT', 'UNAFFECTED')),
    UNIQUE (conversation_id, position)
);

CREATE INDEX idx_conversation_context_items_conversation
    ON conversation_context_items(conversation_id, position);
