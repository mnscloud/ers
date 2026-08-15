CREATE TABLE transaction_types (
    id                  UUID PRIMARY KEY,
    created_at          TIMESTAMP,
    created_by          VARCHAR(100),
    updated_at          TIMESTAMP,
    updated_by          VARCHAR(100),
    version             BIGINT NOT NULL DEFAULT 0,
    code                VARCHAR(50) NOT NULL UNIQUE,
    name                VARCHAR(200) NOT NULL,
    description         VARCHAR(500),
    active              BOOLEAN NOT NULL DEFAULT FALSE,
    approval_status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approval_request_id UUID
);

CREATE TABLE gl_accounts (
    id                  UUID PRIMARY KEY,
    created_at          TIMESTAMP,
    created_by          VARCHAR(100),
    updated_at          TIMESTAMP,
    updated_by          VARCHAR(100),
    version             BIGINT NOT NULL DEFAULT 0,
    code                VARCHAR(50) NOT NULL UNIQUE,
    name                VARCHAR(200) NOT NULL,
    description         VARCHAR(500),
    active              BOOLEAN NOT NULL DEFAULT FALSE,
    approval_status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approval_request_id UUID
);

CREATE TABLE currencies (
    id                  UUID PRIMARY KEY,
    created_at          TIMESTAMP,
    created_by          VARCHAR(100),
    updated_at          TIMESTAMP,
    updated_by          VARCHAR(100),
    version             BIGINT NOT NULL DEFAULT 0,
    code                VARCHAR(50) NOT NULL UNIQUE,
    name                VARCHAR(200) NOT NULL,
    description         VARCHAR(500),
    active              BOOLEAN NOT NULL DEFAULT FALSE,
    approval_status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approval_request_id UUID
);

CREATE TABLE counterparties (
    id                  UUID PRIMARY KEY,
    created_at          TIMESTAMP,
    created_by          VARCHAR(100),
    updated_at          TIMESTAMP,
    updated_by          VARCHAR(100),
    version             BIGINT NOT NULL DEFAULT 0,
    code                VARCHAR(50) NOT NULL UNIQUE,
    name                VARCHAR(200) NOT NULL,
    description         VARCHAR(500),
    active              BOOLEAN NOT NULL DEFAULT FALSE,
    approval_status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approval_request_id UUID
);
