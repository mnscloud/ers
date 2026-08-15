package com.ers.exception.repository;

import com.ers.exception.domain.BreakStatus;
import com.ers.exception.domain.ReconciliationBreak;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReconciliationBreakRepository extends JpaRepository<ReconciliationBreak, UUID> {

    Page<ReconciliationBreak> findByStatus(BreakStatus status, Pageable pageable);

    Page<ReconciliationBreak> findByAssigneeIgnoreCase(String assignee, Pageable pageable);

    List<ReconciliationBreak> findByReconciliationId(UUID reconciliationId);

    long countByStatus(BreakStatus status);
}
