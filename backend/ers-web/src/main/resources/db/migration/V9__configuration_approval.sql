ALTER TABLE data_sources
    ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN approval_request_id UUID;

ALTER TABLE match_rules
    ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN approval_request_id UUID;

ALTER TABLE reconciliation_templates
    ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN approval_request_id UUID;
