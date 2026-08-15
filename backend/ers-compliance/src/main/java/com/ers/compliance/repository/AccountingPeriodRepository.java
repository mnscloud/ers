package com.ers.compliance.repository;

import com.ers.compliance.domain.AccountingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, UUID> {

    Optional<AccountingPeriod> findByPeriodCode(String periodCode);
}
