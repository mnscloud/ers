package com.ers.masterdata.repository;

import com.ers.masterdata.domain.Counterparty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CounterpartyRepository extends JpaRepository<Counterparty, UUID> {
}
