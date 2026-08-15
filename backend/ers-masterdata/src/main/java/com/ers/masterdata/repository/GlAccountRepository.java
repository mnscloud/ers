package com.ers.masterdata.repository;

import com.ers.masterdata.domain.GlAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GlAccountRepository extends JpaRepository<GlAccount, UUID> {
}
