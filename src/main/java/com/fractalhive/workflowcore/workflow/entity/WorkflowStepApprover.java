package com.fractalhive.workflowcore.workflow.entity;

import com.fractalhive.workflowcore.approval.enums.ApproverType;
import com.fractalhive.workflowcore.common.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Defines approvers for a workflow step.
 * Approvers can be users, roles, or manager chains.
 */
@Entity
@Table(name = "workflow_step_approver")
@Getter
@Setter
public class WorkflowStepApprover extends BaseEntity {

    @Column(name = "step_id", nullable = false, updatable = false)
    private UUID stepId;

    @Enumerated(EnumType.STRING)
    @Column(name = "approver_type", nullable = false, length = 20)
    private ApproverType approverType;

    @Column(name = "approver_value", nullable = false, length = 100)
    private String approverValue;

    /**
     * Optional read-only association to parent step definition.
     * Not used for persistence, only for convenience queries.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", insertable = false, updatable = false)
    private WorkflowStepDefinition stepDefinition;
}
