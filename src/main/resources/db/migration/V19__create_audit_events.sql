-- Append-only business audit trail scoped by company.
CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    company_id VARCHAR(36) NOT NULL,
    username VARCHAR(255) NOT NULL,
    action VARCHAR(60) NOT NULL,
    resource_type VARCHAR(60) NOT NULL,
    resource_id VARCHAR(100) NOT NULL,
    outcome VARCHAR(30) NOT NULL,
    detail VARCHAR(1000),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT audit_events_company_fkey FOREIGN KEY (company_id) REFERENCES companies(id)
);

CREATE INDEX idx_audit_events_company_occurred
    ON audit_events(company_id, occurred_at DESC);
CREATE INDEX idx_audit_events_resource
    ON audit_events(company_id, resource_type, resource_id);
CREATE INDEX idx_audit_events_action
    ON audit_events(company_id, action, occurred_at DESC);

-- Audit records are immutable at database level for the application role.
CREATE OR REPLACE FUNCTION prevent_audit_event_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit events are append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_events_no_update_or_delete
BEFORE UPDATE OR DELETE ON audit_events
FOR EACH ROW EXECUTE FUNCTION prevent_audit_event_mutation();
