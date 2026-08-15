package com.ers.ingestion.repository;

import com.ers.ingestion.domain.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DataSourceRepository extends JpaRepository<DataSource, UUID> {
}
