package com.fractalhive.workflowcore.workflow.entity;

import com.fractalhive.workflowcore.approval.enums.ApprovalType;
import com.fractalhive.workflowcore.common.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Represents a step definition within a workflow.
 * Steps define approval rules, SLA, and execution order.
 */
@Entity
@Table(name = "workflow_step_definition")
@Getter
@Setter
public class WorkflowStepDefinition extends BaseEntity {

    @Column(name = "workflow_id", nullable = false, updatable = false)
    private UUID workflowId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_type", nullable = false, length = 20)
    private ApprovalType approvalType;

    @Column(name = "min_approvals")
    private Integer minApprovals;

    @Column(name = "sla_hours")
    private Integer slaHours;

    /**
     * Optional read-only association to parent workflow.
     * Not used for persistence, only for convenience queries.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", insertable = false, updatable = false)
    private WorkflowDefinition workflowDefinition;

    /**
     * Optional read-only association to approvers.
     * Not used for persistence, only for convenience queries.
     */
    @OneToMany(mappedBy = "stepDefinition", fetch = FetchType.LAZY)
    private List<WorkflowStepApprover> approvers;
}
