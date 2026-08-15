package com.ers.reconciliation.domain;

import com.ers.common.domain.BaseEntity;
import com.ers.common.enums.ReconciliationType;
import com.ers.matching.domain.MatchRun;
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
@Table(name = "reconciliations")
@Getter
@Setter
@NoArgsConstructor
public class Reconciliation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ReconciliationTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationType type;

    /** e.g. "2026-07" */
    @Column(nullable = false, length = 7)
    private String periodCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconStatus status = ReconStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_run_id")
    private MatchRun matchRun;

    private int matchedCount;
    private int unmatchedCount;

    private Instant completedAt;
}
