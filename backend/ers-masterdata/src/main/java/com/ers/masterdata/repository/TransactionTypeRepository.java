package com.ers.masterdata.repository;

import com.ers.masterdata.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionTypeRepository extends JpaRepository<TransactionType, UUID> {
}
