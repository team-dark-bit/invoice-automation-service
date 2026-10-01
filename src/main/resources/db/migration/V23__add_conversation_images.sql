CREATE TABLE conversation_images (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    message_id UUID NOT NULL UNIQUE REFERENCES messages(id) ON DELETE CASCADE,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    format VARCHAR(10) NOT NULL CHECK (format IN ('JPEG', 'PNG', 'WEBP')),
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    width INTEGER NOT NULL CHECK (width > 0),
    height INTEGER NOT NULL CHECK (height > 0),
    sha256 VARCHAR(64) NOT NULL CHECK (sha256 ~ '^[a-f0-9]{64}$'),
    retention_policy VARCHAR(20) NOT NULL CHECK (retention_policy IN ('TEMPORARY', 'PERMANENT')),
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT conversation_images_retention_check CHECK (
        (retention_policy = 'TEMPORARY' AND expires_at IS NOT NULL)
        OR (retention_policy = 'PERMANENT' AND expires_at IS NULL)
    )
);

CREATE UNIQUE INDEX uq_conversation_images_active_hash
    ON conversation_images(conversation_id, sha256)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_conversation_images_expiration
    ON conversation_images(expires_at)
    WHERE deleted_at IS NULL AND expires_at IS NOT NULL;
