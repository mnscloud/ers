package com.ers.masterdata.service;

import com.ers.common.enums.ApprovalStatus;
import com.ers.common.exception.ResourceNotFoundException;
import com.ers.compliance.service.ApprovalService;
import com.ers.masterdata.domain.TransactionType;
import com.ers.masterdata.dto.MasterDataRequest;
import com.ers.masterdata.repository.TransactionTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TransactionTypeService {

    private final TransactionTypeRepository repository;
    private final ApprovalService approvalService;

    public TransactionTypeService(TransactionTypeRepository repository, ApprovalService approvalService) {
        this.repository = repository;
        this.approvalService = approvalService;
    }

    @Transactional
    public TransactionType create(MasterDataRequest request, String requestedBy) {
        TransactionType entity = new TransactionType();
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setActive(false);
        entity.setApprovalStatus(ApprovalStatus.PENDING);
        entity = repository.save(entity);

        var approval = approvalService.requestApproval("TransactionType", entity.getId().toString(),
                "CREATE", requestedBy, null);
        entity.setApprovalRequestId(approval.getId());
        return repository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<TransactionType> list() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public TransactionType get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("TransactionType", id));
    }

    @Transactional
    public TransactionType approve(UUID id, String decidedBy, String comment) {
        TransactionType entity = get(id);
        approvalService.approve(entity.getApprovalRequestId(), decidedBy, comment);
        entity.setApprovalStatus(ApprovalStatus.APPROVED);
        entity.setActive(true);
        return repository.save(entity);
    }

    @Transactional
    public TransactionType reject(UUID id, String decidedBy, String comment) {
        TransactionType entity = get(id);
        approvalService.reject(entity.getApprovalRequestId(), decidedBy, comment);
        entity.setApprovalStatus(ApprovalStatus.REJECTED);
        return repository.save(entity);
    }
}
