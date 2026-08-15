package com.ers.matching.repository;

import com.ers.matching.domain.MatchRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatchRunRepository extends JpaRepository<MatchRun, UUID> {

    Page<MatchRun> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
