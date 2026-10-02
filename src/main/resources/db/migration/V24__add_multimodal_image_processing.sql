ALTER TABLE conversation_images
    ADD COLUMN processing_status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    ADD COLUMN processing_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN processing_started_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN processed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN last_processing_error VARCHAR(1000),
    ADD COLUMN interpretation_json TEXT,
    ADD CONSTRAINT conversation_images_processing_status_check CHECK (
        processing_status IN ('RECEIVED', 'PROCESSING', 'EXTRACTED', 'FAILED')
    ),
    ADD CONSTRAINT conversation_images_processing_attempts_check CHECK (
        processing_attempts >= 0
    ),
    ADD CONSTRAINT conversation_images_processing_state_check CHECK (
        (processing_status = 'RECEIVED'
            AND processing_started_at IS NULL AND processed_at IS NULL
            AND last_processing_error IS NULL AND interpretation_json IS NULL)
        OR (processing_status = 'PROCESSING'
            AND processing_started_at IS NOT NULL AND processed_at IS NULL
            AND last_processing_error IS NULL AND interpretation_json IS NULL
            AND processing_attempts > 0)
        OR (processing_status = 'EXTRACTED'
            AND processing_started_at IS NOT NULL AND processed_at IS NOT NULL
            AND last_processing_error IS NULL AND interpretation_json IS NOT NULL
            AND processing_attempts > 0)
        OR (processing_status = 'FAILED'
            AND processing_started_at IS NOT NULL AND processed_at IS NOT NULL
            AND last_processing_error IS NOT NULL AND interpretation_json IS NULL
            AND processing_attempts > 0)
    );

CREATE INDEX idx_conversation_images_processing_status
    ON conversation_images(processing_status, created_at)
    WHERE deleted_at IS NULL;
