package com.fractalhive.workflowcore.approval.entity;

import com.fractalhive.workflowcore.approval.enums.DecisionType;
import com.fractalhive.workflowcore.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Represents an immutable record of an approval decision.
 * Each approval task can have one decision record.
 */
@Entity
@Table(name = "approval_decision")
@Getter
@Setter
public class ApprovalDecision extends BaseEntity {

    @Column(name = "approval_task_id", nullable = false, updatable = false)
    private UUID approvalTaskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private DecisionType decision;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "decided_by", nullable = false, length = 100)
    private String decidedBy;

    @Column(name = "decided_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Timestamp decidedAt;

    /**
     * Optional read-only association to parent approval task.
     * Not used for persistence, only for convenience queries.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_task_id", insertable = false, updatable = false)
    private ApprovalTask approvalTask;
}
