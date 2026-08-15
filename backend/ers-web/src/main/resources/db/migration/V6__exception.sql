CREATE TABLE reconciliation_breaks (
    id                      UUID PRIMARY KEY,
    created_at              TIMESTAMP,
    created_by              VARCHAR(100),
    updated_at              TIMESTAMP,
    updated_by              VARCHAR(100),
    version                 BIGINT NOT NULL DEFAULT 0,
    reconciliation_id       UUID NOT NULL REFERENCES reconciliations(id),
    record_id               UUID REFERENCES raw_transaction_records(id),
    category                VARCHAR(30) NOT NULL,
    severity                VARCHAR(20) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    assignee                VARCHAR(255),
    sla_due_date            DATE,
    description             VARCHAR(1000),
    resolution_comment      VARCHAR(1000)
);
CREATE INDEX idx_reconciliation_breaks_status ON reconciliation_breaks(status);
CREATE INDEX idx_reconciliation_breaks_assignee ON reconciliation_breaks(assignee);
