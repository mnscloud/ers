CREATE TABLE journal_entries (
    id                          UUID PRIMARY KEY,
    created_at                  TIMESTAMP,
    created_by                  VARCHAR(100),
    updated_at                  TIMESTAMP,
    updated_by                  VARCHAR(100),
    version                     BIGINT NOT NULL DEFAULT 0,
    reconciliation_break_id     UUID REFERENCES reconciliation_breaks(id),
    account_code                VARCHAR(50) NOT NULL,
    debit_credit                VARCHAR(10) NOT NULL,
    amount                      NUMERIC(19,4) NOT NULL,
    currency                    VARCHAR(3) NOT NULL,
    description                 VARCHAR(500) NOT NULL,
    period_code                 VARCHAR(7) NOT NULL,
    status                      VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    approval_request_id         UUID,
    erp_reference                VARCHAR(255),
    posted_at                   TIMESTAMP
);
CREATE INDEX idx_journal_entries_status ON journal_entries(status);
