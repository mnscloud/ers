package com.ers.exception.service;

import com.ers.common.enums.AuditAction;
import com.ers.common.event.AuditLogEvent;
import com.ers.common.exception.BusinessException;
import com.ers.common.exception.ResourceNotFoundException;
import com.ers.exception.domain.BreakStatus;
import com.ers.exception.domain.ReconciliationBreak;
import com.ers.exception.repository.ReconciliationBreakRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BreakTriageService {

    private final ReconciliationBreakRepository breakRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BreakTriageService(ReconciliationBreakRepository breakRepository, ApplicationEventPublisher eventPublisher) {
        this.breakRepository = breakRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public Page<ReconciliationBreak> findByStatus(BreakStatus status, Pageable pageable) {
        return breakRepository.findByStatus(status, pageable);
    }

    @Transactional
    public ReconciliationBreak assign(UUID id, String assignee, String actor) {
        ReconciliationBreak recBreak = get(id);
        recBreak.setAssignee(assignee);
        recBreak.setStatus(BreakStatus.IN_REVIEW);
        ReconciliationBreak saved = breakRepository.save(recBreak);
        eventPublisher.publishEvent(AuditLogEvent.of(actor, AuditAction.ASSIGN, "ReconciliationBreak",
                id.toString(), "Assigned to " + assignee));
        return saved;
    }

    @Transactional
    public ReconciliationBreak escalate(UUID id, String actor) {
        ReconciliationBreak recBreak = get(id);
        recBreak.setStatus(BreakStatus.ESCALATED);
        ReconciliationBreak saved = breakRepository.save(recBreak);
        eventPublisher.publishEvent(AuditLogEvent.of(actor, AuditAction.ESCALATE, "ReconciliationBreak",
                id.toString(), "Escalated"));
        return saved;
    }

    @Transactional
    public ReconciliationBreak resolve(UUID id, String resolutionComment, String actor) {
        ReconciliationBreak recBreak = get(id);
        if (recBreak.getStatus() == BreakStatus.RESOLVED) {
            throw new BusinessException("ALREADY_RESOLVED", "This break is already resolved");
        }
        recBreak.setStatus(BreakStatus.RESOLVED);
        recBreak.setResolutionComment(resolutionComment);
        ReconciliationBreak saved = breakRepository.save(recBreak);
        eventPublisher.publishEvent(AuditLogEvent.of(actor, AuditAction.RESOLVE, "ReconciliationBreak",
                id.toString(), resolutionComment));
        return saved;
    }

    private ReconciliationBreak get(UUID id) {
        return breakRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ReconciliationBreak", id));
    }
}
