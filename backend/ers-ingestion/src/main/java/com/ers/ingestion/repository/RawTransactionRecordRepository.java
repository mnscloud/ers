package com.ers.ingestion.repository;

import com.ers.ingestion.domain.RawTransactionRecord;
import com.ers.ingestion.domain.RecordMatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RawTransactionRecordRepository extends JpaRepository<RawTransactionRecord, UUID> {

    Page<RawTransactionRecord> findByBatchId(UUID batchId, Pageable pageable);

    List<RawTransactionRecord> findBySourceSystemAndMatchStatusAndTransactionDateBetween(
            String sourceSystem, RecordMatchStatus matchStatus, LocalDate from, LocalDate to);

    long countBySourceSystemAndMatchStatus(String sourceSystem, RecordMatchStatus matchStatus);
}
