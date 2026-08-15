package com.ers.compliance.repository;

import com.ers.compliance.domain.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    Page<AuditEvent> findByEntityTypeIgnoreCaseOrderByOccurredAtDesc(String entityType, Pageable pageable);

    Page<AuditEvent> findByActorIgnoreCaseOrderByOccurredAtDesc(String actor, Pageable pageable);

    Page<AuditEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
