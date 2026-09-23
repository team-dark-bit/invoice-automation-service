CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    company_id VARCHAR(36) NOT NULL REFERENCES companies(id),
    customer_id VARCHAR(36) REFERENCES customers(id),
    invoice_draft_id UUID REFERENCES invoice_draft(id),
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('REST', 'WHATSAPP', 'WEB', 'INTERNAL')),
    external_participant_id VARCHAR(150),
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT conversations_closed_state_check CHECK (
        (status = 'OPEN' AND closed_at IS NULL)
        OR (status = 'CLOSED' AND closed_at IS NOT NULL)
    )
);

CREATE INDEX idx_conversations_company_updated
    ON conversations(company_id, updated_at DESC);
CREATE INDEX idx_conversations_participant
    ON conversations(company_id, channel, external_participant_id);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    direction VARCHAR(20) NOT NULL CHECK (direction IN ('INBOUND', 'OUTBOUND', 'SYSTEM')),
    type VARCHAR(20) NOT NULL CHECK (type IN ('TEXT', 'IMAGE', 'DOCUMENT', 'SYSTEM')),
    content VARCHAR(4000),
    media_url VARCHAR(1000),
    external_message_id VARCHAR(255),
    status VARCHAR(20) NOT NULL CHECK (
        status IN ('RECEIVED', 'PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED')
    ),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT messages_payload_check CHECK (content IS NOT NULL OR media_url IS NOT NULL)
);

CREATE INDEX idx_messages_conversation_created
    ON messages(conversation_id, created_at ASC);
CREATE UNIQUE INDEX uq_messages_external_id
    ON messages(conversation_id, external_message_id)
    WHERE external_message_id IS NOT NULL;
