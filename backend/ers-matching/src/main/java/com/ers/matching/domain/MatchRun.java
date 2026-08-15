package com.ers.matching.domain;

import com.ers.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "match_runs")
@Getter
@Setter
@NoArgsConstructor
public class MatchRun extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_rule_id", nullable = false)
    private MatchRule matchRule;

    /** e.g. "2026-07" - the accounting period this run covers. */
    @Column(nullable = false, length = 7)
    private String periodCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunStatus status = RunStatus.PENDING;

    private int matchedCount;

    @Column(name = "unmatched_count_a")
    private int unmatchedCountA;

    @Column(name = "unmatched_count_b")
    private int unmatchedCountB;

    private Instant startedAt;
    private Instant completedAt;

    @Column(length = 2000)
    private String errorMessage;
}
