package com.ers.reconciliation.domain;

import com.ers.common.domain.ApprovableEntity;
import com.ers.common.enums.ReconciliationType;
import com.ers.matching.domain.MatchRule;
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

@Entity
@Table(name = "reconciliation_templates")
@Getter
@Setter
@NoArgsConstructor
public class ReconciliationTemplate extends ApprovableEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_rule_id")
    private MatchRule matchRule;

    @Column(nullable = false, length = 100)
    private String owner;

    @Column(nullable = false)
    private boolean active = false;
}
