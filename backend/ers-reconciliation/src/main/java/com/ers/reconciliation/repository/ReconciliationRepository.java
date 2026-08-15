package com.ers.reconciliation.repository;

import com.ers.reconciliation.domain.ReconStatus;
import com.ers.reconciliation.domain.Reconciliation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReconciliationRepository extends JpaRepository<Reconciliation, UUID> {

    Page<Reconciliation> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(ReconStatus status);
}
