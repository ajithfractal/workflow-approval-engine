package com.fractalhive.workflowcore.workflow.entity;

import com.fractalhive.workflowcore.common.entity.BaseEntity;
import com.fractalhive.workflowcore.workflow.enums.StepStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Represents a runtime instance of a workflow step execution.
 * Tracks the status and timing of a step within a workflow instance.
 */
@Entity
@Table(name = "workflow_step_instance")
@Getter
@Setter
public class WorkflowStepInstance extends BaseEntity {

    @Column(name = "workflow_instance_id", nullable = false, updatable = false)
    private UUID workflowInstanceId;

    @Column(name = "step_id", nullable = false, updatable = false)
    private UUID stepId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StepStatus status = StepStatus.NOT_STARTED;

    @Column(name = "started_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Timestamp startedAt;

    @Column(name = "completed_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Timestamp completedAt;

    /**
     * Optional read-only association to parent workflow instance.
     * Not used for persistence, only for convenience queries.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_instance_id", insertable = false, updatable = false)
    private WorkflowInstance workflowInstance;
}
