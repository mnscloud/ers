package com.ers.compliance.listener;

import com.ers.common.event.AuditLogEvent;
import com.ers.compliance.domain.AuditEvent;
import com.ers.compliance.repository.AuditEventRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditEventListener {

    private final AuditEventRepository auditEventRepository;

    public AuditEventListener(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAuditLogEvent(AuditLogEvent event) {
        auditEventRepository.save(new AuditEvent(
                event.occurredAt(), event.actor(), event.action(),
                event.entityType(), event.entityId(), event.summary()));
    }
}
