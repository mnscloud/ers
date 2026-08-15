package com.ers.matching.service;

import com.ers.common.enums.ApprovalStatus;
import com.ers.common.exception.ResourceNotFoundException;
import com.ers.compliance.service.ApprovalService;
import com.ers.matching.domain.MatchRule;
import com.ers.matching.dto.MatchRuleRequest;
import com.ers.matching.repository.MatchRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MatchRuleService {

    private final MatchRuleRepository matchRuleRepository;
    private final ApprovalService approvalService;

    public MatchRuleService(MatchRuleRepository matchRuleRepository, ApprovalService approvalService) {
        this.matchRuleRepository = matchRuleRepository;
        this.approvalService = approvalService;
    }

    @Transactional
    public MatchRule create(MatchRuleRequest request, String requestedBy) {
        MatchRule rule = new MatchRule();
        rule.setName(request.name());
        rule.setSourceSystemA(request.sourceSystemA());
        rule.setSourceSystemB(request.sourceSystemB());
        rule.setMatchType(request.matchType());
        rule.setAmountTolerance(request.amountTolerance());
        rule.setDateToleranceDays(request.dateToleranceDays());
        rule.setActive(false);
        rule.setApprovalStatus(ApprovalStatus.PENDING);
        rule = matchRuleRepository.save(rule);

        var approval = approvalService.requestApproval("MatchRule", rule.getId().toString(),
                "CREATE", requestedBy, null);
        rule.setApprovalRequestId(approval.getId());
        return matchRuleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public List<MatchRule> list() {
        return matchRuleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public MatchRule get(UUID id) {
        return matchRuleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("MatchRule", id));
    }

    @Transactional
    public MatchRule approve(UUID id, String decidedBy, String comment) {
        MatchRule rule = get(id);
        approvalService.approve(rule.getApprovalRequestId(), decidedBy, comment);
        rule.setApprovalStatus(ApprovalStatus.APPROVED);
        rule.setActive(true);
        return matchRuleRepository.save(rule);
    }

    @Transactional
    public MatchRule reject(UUID id, String decidedBy, String comment) {
        MatchRule rule = get(id);
        approvalService.reject(rule.getApprovalRequestId(), decidedBy, comment);
        rule.setApprovalStatus(ApprovalStatus.REJECTED);
        return matchRuleRepository.save(rule);
    }
}
