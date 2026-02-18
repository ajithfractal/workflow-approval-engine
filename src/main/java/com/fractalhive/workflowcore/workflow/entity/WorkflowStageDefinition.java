package com.fractalhive.workflowcore.workflow.entity;

import com.fractalhive.workflowcore.approval.enums.ApprovalType;
import com.fractalhive.workflowcore.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Represents a stage definition within a workflow.
 * Stages contain steps and define how steps within the stage should complete.
 */
@Entity
@Table(name = "workflow_stage_definition")
@Getter
@Setter
public class WorkflowStageDefinition extends BaseEntity {

    @Column(name = "workflow_id", nullable = false, updatable = false)
    private UUID workflowId;

    @Column(name = "stage_order", nullable = false)
    private Integer stageOrder;

    @Column(name = "stage_name", nullable = false, length = 100)
    private String stageName;

    /**
     * Defines how steps within this stage should complete.
     * ALL: All steps must complete (sequential execution)
     * ANY: Any step completion completes the stage (parallel execution)
     * N_OF_M: Minimum number of steps must complete (parallel execution)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "step_completion_type", nullable = false, length = 20)
    private ApprovalType stepCompletionType;

    /**
     * Minimum number of step completions required (for N_OF_M type).
     */
    @Column(name = "min_step_completions")
    private Integer minStepCompletions;

    /**
     * Optional read-only association to parent workflow.
     * Not used for persistence, only for convenience queries.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", insertable = false, updatable = false)
    private WorkflowDefinition workflowDefinition;

    /**
     * Optional read-only association to steps.
     * Not used for persistence, only for convenience queries.
     */
    @OneToMany(mappedBy = "stageDefinition", fetch = FetchType.LAZY)
    private List<WorkflowStepDefinition> steps;
}
