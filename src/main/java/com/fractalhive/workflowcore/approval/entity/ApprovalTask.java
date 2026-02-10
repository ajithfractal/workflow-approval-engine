package com.fractalhive.workflowcore.approval.entity;

import com.fractalhive.workflowcore.approval.enums.ApproverType;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import com.fractalhive.workflowcore.common.entity.BaseEntity;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepInstance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Represents an approval task assigned to an approver for a workflow step.
 */
@Entity
@Table(name = "approval_task")
@Getter
@Setter
public class ApprovalTask extends BaseEntity {

    @Column(name = "step_instance_id", nullable = false, updatable = false)
    private UUID stepInstanceId;

    @Column(name = "approver_id", nullable = false, length = 100)
    private String approverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "approver_type", nullable = false, length = 20)
    private ApproverType approverType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TaskStatus status = TaskStatus.PENDING;

    @Column(name = "due_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Timestamp dueAt;

    @Column(name = "acted_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Timestamp actedAt;

    /**
     * Optional read-only association to parent step instance.
     * Not used for persistence, only for convenience queries.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_instance_id", insertable = false, updatable = false)
    private WorkflowStepInstance stepInstance;

    /**
     * Optional read-only association to decisions.
     * Not used for persistence, only for convenience queries.
     */
    @OneToMany(mappedBy = "approvalTask", fetch = FetchType.LAZY)
    private List<ApprovalDecision> decisions;

    /**
     * Optional read-only association to comments.
     * Not used for persistence, only for convenience queries.
     */
    @OneToMany(mappedBy = "approvalTask", fetch = FetchType.LAZY)
    private List<ApprovalComment> comments;
}
