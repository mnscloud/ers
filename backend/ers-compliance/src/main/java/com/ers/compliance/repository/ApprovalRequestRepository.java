package com.ers.compliance.repository;

import com.ers.common.enums.ApprovalStatus;
import com.ers.compliance.domain.ApprovalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {

    Page<ApprovalRequest> findByStatus(ApprovalStatus status, Pageable pageable);

    List<ApprovalRequest> findByEntityTypeAndEntityId(String entityType, String entityId);
}
