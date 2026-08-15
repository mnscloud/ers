CREATE TABLE audit_events (
    id              UUID PRIMARY KEY,
    occurred_at     TIMESTAMP NOT NULL,
    actor           VARCHAR(100) NOT NULL,
    action          VARCHAR(30) NOT NULL,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       VARCHAR(100),
    summary         VARCHAR(1000)
);
CREATE INDEX idx_audit_events_entity_type ON audit_events(entity_type);
CREATE INDEX idx_audit_events_actor ON audit_events(actor);
CREATE INDEX idx_audit_events_occurred_at ON audit_events(occurred_at);

CREATE TABLE accounting_periods (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_at      TIMESTAMP,
    updated_by      VARCHAR(100),
    version         BIGINT NOT NULL DEFAULT 0,
    period_code     VARCHAR(7) NOT NULL UNIQUE,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    status          VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    locked_by       VARCHAR(255),
    locked_at       TIMESTAMP
);

CREATE TABLE approval_requests (
    id                  UUID PRIMARY KEY,
    created_at          TIMESTAMP,
    created_by          VARCHAR(100),
    updated_at          TIMESTAMP,
    updated_by          VARCHAR(100),
    version             BIGINT NOT NULL DEFAULT 0,
    entity_type         VARCHAR(100) NOT NULL,
    entity_id           VARCHAR(100) NOT NULL,
    action              VARCHAR(50) NOT NULL,
    requested_by        VARCHAR(100) NOT NULL,
    request_comment     VARCHAR(1000),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decided_by          VARCHAR(255),
    decided_at          TIMESTAMP,
    decision_comment    VARCHAR(1000)
);
CREATE INDEX idx_approval_requests_status ON approval_requests(status);
