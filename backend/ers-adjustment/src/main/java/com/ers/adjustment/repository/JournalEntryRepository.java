package com.ers.adjustment.repository;

import com.ers.adjustment.domain.JournalEntry;
import com.ers.adjustment.domain.JournalEntryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    Page<JournalEntry> findByStatus(JournalEntryStatus status, Pageable pageable);

    Page<JournalEntry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(JournalEntryStatus status);
}
