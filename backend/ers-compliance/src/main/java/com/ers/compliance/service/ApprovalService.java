package com.ers.compliance.service;

import com.ers.common.enums.ApprovalStatus;
import com.ers.common.enums.AuditAction;
import com.ers.common.event.AuditLogEvent;
import com.ers.common.exception.BusinessException;
import com.ers.common.exception.ResourceNotFoundException;
import com.ers.compliance.domain.ApprovalRequest;
import com.ers.compliance.repository.ApprovalRequestRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Generic single-level maker-checker engine reused by other modules (e.g. adjustment posting).
 * The maker who created the request may never also be the checker who decides it.
 */
@Service
public class ApprovalService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ApprovalService(ApprovalRequestRepository approvalRequestRepository, ApplicationEventPublisher eventPublisher) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ApprovalRequest requestApproval(String entityType, String entityId, String action,
                                            String requestedBy, String comment) {
        ApprovalRequest request = new ApprovalRequest();
        request.setEntityType(entityType);
        request.setEntityId(entityId);
        request.setAction(action);
        request.setRequestedBy(requestedBy);
        request.setRequestComment(comment);
        request.setStatus(ApprovalStatus.PENDING);
        return approvalRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public Page<ApprovalRequest> findByStatus(ApprovalStatus status, Pageable pageable) {
        return approvalRequestRepository.findByStatus(status, pageable);
    }

    @Transactional
    public ApprovalRequest approve(UUID requestId, String decidedBy, String comment) {
        return decide(requestId, decidedBy, comment, ApprovalStatus.APPROVED);
    }

    @Transactional
    public ApprovalRequest reject(UUID requestId, String decidedBy, String comment) {
        return decide(requestId, decidedBy, comment, ApprovalStatus.REJECTED);
    }

    private ApprovalRequest decide(UUID requestId, String decidedBy, String comment, ApprovalStatus outcome) {
        ApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> ResourceNotFoundException.of("ApprovalRequest", requestId));
        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException("APPROVAL_ALREADY_DECIDED", "This approval request has already been decided");
        }
        if (request.getRequestedBy().equalsIgnoreCase(decidedBy)) {
            throw new BusinessException("MAKER_CHECKER_VIOLATION", "The requester cannot also approve or reject their own request");
        }
        request.setStatus(outcome);
        request.setDecidedBy(decidedBy);
        request.setDecidedAt(Instant.now());
        request.setDecisionComment(comment);
        ApprovalRequest saved = approvalRequestRepository.save(request);

        eventPublisher.publishEvent(AuditLogEvent.of(decidedBy,
                outcome == ApprovalStatus.APPROVED ? AuditAction.APPROVE : AuditAction.REJECT,
                request.getEntityType(), request.getEntityId(),
                request.getAction() + " " + outcome.name().toLowerCase()));

        return saved;
    }
}
