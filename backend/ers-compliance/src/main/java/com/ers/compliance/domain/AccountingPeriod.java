package com.ers.compliance.domain;

import com.ers.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "accounting_periods")
@Getter
@Setter
@NoArgsConstructor
public class AccountingPeriod extends BaseEntity {

    /** e.g. "2026-07" */
    @Column(nullable = false, unique = true, length = 7)
    private String periodCode;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PeriodStatus status = PeriodStatus.OPEN;

    private String lockedBy;

    private Instant lockedAt;
}
