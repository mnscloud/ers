CREATE TABLE data_sources (
    id                  UUID PRIMARY KEY,
    created_at          TIMESTAMP,
    created_by          VARCHAR(100),
    updated_at          TIMESTAMP,
    updated_by          VARCHAR(100),
    version             BIGINT NOT NULL DEFAULT 0,
    name                VARCHAR(100) NOT NULL UNIQUE,
    source_system       VARCHAR(100) NOT NULL,
    type                VARCHAR(20) NOT NULL,
    default_format      VARCHAR(10) NOT NULL,
    connection_config    VARCHAR(1000),
    active              BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE ingestion_batches (
    id                  UUID PRIMARY KEY,
    created_at          TIMESTAMP,
    created_by          VARCHAR(100),
    updated_at          TIMESTAMP,
    updated_by          VARCHAR(100),
    version             BIGINT NOT NULL DEFAULT 0,
    data_source_id      UUID NOT NULL REFERENCES data_sources(id),
    file_name           VARCHAR(255) NOT NULL,
    format              VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_records       INTEGER NOT NULL DEFAULT 0,
    success_records     INTEGER NOT NULL DEFAULT 0,
    failed_records      INTEGER NOT NULL DEFAULT 0,
    error_message       VARCHAR(2000),
    started_at          TIMESTAMP,
    completed_at        TIMESTAMP
);

CREATE TABLE raw_transaction_records (
    id                  UUID PRIMARY KEY,
    created_at          TIMESTAMP,
    created_by          VARCHAR(100),
    updated_at          TIMESTAMP,
    updated_by          VARCHAR(100),
    version             BIGINT NOT NULL DEFAULT 0,
    batch_id            UUID NOT NULL REFERENCES ingestion_batches(id),
    source_system       VARCHAR(100) NOT NULL,
    external_id         VARCHAR(150) NOT NULL,
    transaction_date    DATE NOT NULL,
    amount              NUMERIC(19,4) NOT NULL,
    currency            VARCHAR(3) NOT NULL,
    description         VARCHAR(500),
    match_status        VARCHAR(20) NOT NULL DEFAULT 'UNMATCHED',
    raw_payload         JSONB
);
CREATE INDEX idx_rtr_source_system ON raw_transaction_records(source_system);
CREATE INDEX idx_rtr_match_status ON raw_transaction_records(match_status);
CREATE INDEX idx_rtr_transaction_date ON raw_transaction_records(transaction_date);
