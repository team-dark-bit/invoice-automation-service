CREATE TABLE invoice_draft (
    id UUID PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
