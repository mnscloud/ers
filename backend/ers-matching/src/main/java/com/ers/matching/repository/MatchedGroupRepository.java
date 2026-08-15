package com.ers.matching.repository;

import com.ers.matching.domain.MatchedGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatchedGroupRepository extends JpaRepository<MatchedGroup, UUID> {

    Page<MatchedGroup> findByMatchRunId(UUID matchRunId, Pageable pageable);
}
