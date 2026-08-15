CREATE TABLE match_rules (
    id                      UUID PRIMARY KEY,
    created_at              TIMESTAMP,
    created_by              VARCHAR(100),
    updated_at              TIMESTAMP,
    updated_by              VARCHAR(100),
    version                 BIGINT NOT NULL DEFAULT 0,
    name                    VARCHAR(100) NOT NULL UNIQUE,
    source_system_a         VARCHAR(100) NOT NULL,
    source_system_b         VARCHAR(100) NOT NULL,
    match_type              VARCHAR(20) NOT NULL DEFAULT 'ONE_TO_ONE',
    amount_tolerance        NUMERIC(19,4) NOT NULL DEFAULT 0,
    date_tolerance_days     INTEGER NOT NULL DEFAULT 0,
    active                  BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE match_runs (
    id                  UUID PRIMARY KEY,
    created_at          TIMESTAMP,
    created_by          VARCHAR(100),
    updated_at          TIMESTAMP,
    updated_by          VARCHAR(100),
    version             BIGINT NOT NULL DEFAULT 0,
    match_rule_id       UUID NOT NULL REFERENCES match_rules(id),
    period_code         VARCHAR(7) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    matched_count       INTEGER NOT NULL DEFAULT 0,
    unmatched_count_a   INTEGER NOT NULL DEFAULT 0,
    unmatched_count_b   INTEGER NOT NULL DEFAULT 0,
    started_at          TIMESTAMP,
    completed_at        TIMESTAMP,
    error_message       VARCHAR(2000)
);

CREATE TABLE matched_groups (
    id                  UUID PRIMARY KEY,
    created_at          TIMESTAMP,
    created_by          VARCHAR(100),
    updated_at          TIMESTAMP,
    updated_by          VARCHAR(100),
    version             BIGINT NOT NULL DEFAULT 0,
    match_run_id        UUID NOT NULL REFERENCES match_runs(id),
    match_type          VARCHAR(20) NOT NULL,
    confidence_score    DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE TABLE matched_group_records (
    matched_group_id    UUID NOT NULL REFERENCES matched_groups(id),
    record_id           UUID NOT NULL REFERENCES raw_transaction_records(id),
    PRIMARY KEY (matched_group_id, record_id)
);
