package com.fractalhive.workflowcore.workflow.entity;

import com.fractalhive.workflowcore.common.entity.BaseEntity;
import com.fractalhive.workflowcore.workflow.enums.StageStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Represents a runtime instance of a workflow stage execution.
 * Tracks the status and timing of a stage within a workflow instance.
 */
@Entity
@Table(name = "workflow_stage_instance")
@Getter
@Setter
public class WorkflowStageInstance extends BaseEntity {

    @Column(name = "workflow_instance_id", nullable = false, updatable = false)
    private UUID workflowInstanceId;

    @Column(name = "stage_id", nullable = false, updatable = false)
    private UUID stageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StageStatus status = StageStatus.NOT_STARTED;

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
