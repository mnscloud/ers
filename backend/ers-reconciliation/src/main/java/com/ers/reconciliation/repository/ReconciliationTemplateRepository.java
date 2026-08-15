package com.ers.reconciliation.repository;

import com.ers.reconciliation.domain.ReconciliationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReconciliationTemplateRepository extends JpaRepository<ReconciliationTemplate, UUID> {
}
