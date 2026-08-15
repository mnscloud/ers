package com.ers.matching.domain;

import com.ers.common.domain.ApprovableEntity;
import com.ers.common.enums.MatchType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "match_rules")
@Getter
@Setter
@NoArgsConstructor
public class MatchRule extends ApprovableEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "source_system_a", nullable = false, length = 100)
    private String sourceSystemA;

    @Column(name = "source_system_b", nullable = false, length = 100)
    private String sourceSystemB;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchType matchType = MatchType.ONE_TO_ONE;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amountTolerance = BigDecimal.ZERO;

    @Column(nullable = false)
    private int dateToleranceDays = 0;

    @Column(nullable = false)
    private boolean active = false;
}
