package com.ers.matching.repository;

import com.ers.matching.domain.MatchRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatchRuleRepository extends JpaRepository<MatchRule, UUID> {
}
