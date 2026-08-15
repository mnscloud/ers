CREATE TABLE reconciliation_templates (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_at      TIMESTAMP,
    updated_by      VARCHAR(100),
    version         BIGINT NOT NULL DEFAULT 0,
    name            VARCHAR(150) NOT NULL UNIQUE,
    type            VARCHAR(20) NOT NULL,
    match_rule_id   UUID REFERENCES match_rules(id),
    owner           VARCHAR(100) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE reconciliations (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_at      TIMESTAMP,
    updated_by      VARCHAR(100),
    version         BIGINT NOT NULL DEFAULT 0,
    template_id     UUID NOT NULL REFERENCES reconciliation_templates(id),
    type            VARCHAR(20) NOT NULL,
    period_code     VARCHAR(7) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    match_run_id    UUID REFERENCES match_runs(id),
    matched_count   INTEGER NOT NULL DEFAULT 0,
    unmatched_count INTEGER NOT NULL DEFAULT 0,
    completed_at    TIMESTAMP
);
CREATE INDEX idx_reconciliations_status ON reconciliations(status);
