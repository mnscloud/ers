package com.ers.masterdata.service;

import com.ers.common.enums.ApprovalStatus;
import com.ers.common.exception.ResourceNotFoundException;
import com.ers.compliance.service.ApprovalService;
import com.ers.masterdata.domain.Counterparty;
import com.ers.masterdata.dto.MasterDataRequest;
import com.ers.masterdata.repository.CounterpartyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CounterpartyService {

    private final CounterpartyRepository repository;
    private final ApprovalService approvalService;

    public CounterpartyService(CounterpartyRepository repository, ApprovalService approvalService) {
        this.repository = repository;
        this.approvalService = approvalService;
    }

    @Transactional
    public Counterparty create(MasterDataRequest request, String requestedBy) {
        Counterparty entity = new Counterparty();
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setActive(false);
        entity.setApprovalStatus(ApprovalStatus.PENDING);
        entity = repository.save(entity);

        var approval = approvalService.requestApproval("Counterparty", entity.getId().toString(),
                "CREATE", requestedBy, null);
        entity.setApprovalRequestId(approval.getId());
        return repository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<Counterparty> list() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Counterparty get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Counterparty", id));
    }

    @Transactional
    public Counterparty approve(UUID id, String decidedBy, String comment) {
        Counterparty entity = get(id);
        approvalService.approve(entity.getApprovalRequestId(), decidedBy, comment);
        entity.setApprovalStatus(ApprovalStatus.APPROVED);
        entity.setActive(true);
        return repository.save(entity);
    }

    @Transactional
    public Counterparty reject(UUID id, String decidedBy, String comment) {
        Counterparty entity = get(id);
        approvalService.reject(entity.getApprovalRequestId(), decidedBy, comment);
        entity.setApprovalStatus(ApprovalStatus.REJECTED);
        return repository.save(entity);
    }
}
