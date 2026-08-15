package com.ers.common.domain;

import com.ers.common.enums.ApprovalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Base for configuration/master-data entities that require maker-checker approval
 * before taking effect. Pairs with ApprovalService in ers-compliance: entities start
 * PENDING on creation and only become usable once approved by a different user.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class ApprovableEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "approval_request_id")
    private UUID approvalRequestId;
}
