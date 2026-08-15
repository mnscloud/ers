package com.ers.common.event;

import com.ers.common.enums.AuditAction;

import java.time.Instant;

public record AuditLogEvent(
        String actor,
        AuditAction action,
        String entityType,
        String entityId,
        String summary,
        Instant occurredAt
) {
    public static AuditLogEvent of(String actor, AuditAction action, String entityType, String entityId, String summary) {
        return new AuditLogEvent(actor, action, entityType, entityId, summary, Instant.now());
    }
}
