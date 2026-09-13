ALTER TABLE invoice_draft
    ADD COLUMN company_id VARCHAR(36),
    ADD COLUMN customer_id VARCHAR(36),
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'PEN',
    ADD COLUMN subtotal NUMERIC(20, 6) NOT NULL DEFAULT 0,
    ADD COLUMN total NUMERIC(20, 6) NOT NULL DEFAULT 0,
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;

UPDATE invoice_draft SET updated_at = created_at WHERE updated_at IS NULL;

ALTER TABLE invoice_draft
    ALTER COLUMN updated_at SET NOT NULL,
    ADD CONSTRAINT fk_invoice_draft_company FOREIGN KEY (company_id) REFERENCES companies(id),
    ADD CONSTRAINT fk_invoice_draft_customer FOREIGN KEY (customer_id) REFERENCES customers(id);

CREATE TABLE invoice_items (
    id UUID PRIMARY KEY,
    invoice_draft_id UUID NOT NULL,
    position INTEGER NOT NULL CHECK (position >= 0),
    description VARCHAR(500) NOT NULL,
    quantity NUMERIC(16, 4) NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(16, 2) NOT NULL CHECK (unit_price >= 0),
    line_total NUMERIC(20, 6) NOT NULL CHECK (line_total >= 0),
    CONSTRAINT fk_invoice_items_invoice_draft
        FOREIGN KEY (invoice_draft_id) REFERENCES invoice_draft(id) ON DELETE CASCADE
);

CREATE INDEX idx_invoice_items_invoice_draft_id ON invoice_items(invoice_draft_id);
CREATE UNIQUE INDEX idx_invoice_items_draft_position ON invoice_items(invoice_draft_id, position);
