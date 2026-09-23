ALTER TABLE electronic_documents
    ADD COLUMN emission_at TIMESTAMP WITH TIME ZONE;

UPDATE electronic_documents
SET emission_at = COALESCE(submitted_at, responded_at, CURRENT_TIMESTAMP)
WHERE emission_at IS NULL;

ALTER TABLE electronic_documents
    ALTER COLUMN emission_at SET NOT NULL;
