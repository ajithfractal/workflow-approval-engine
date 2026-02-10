package com.fractalhive.workflowcore.workflow.entity;

import com.fractalhive.workflowcore.common.entity.BaseEntity;
import com.fractalhive.workflowcore.workflow.enums.WorkflowStatus;
import com.fractalhive.workflowcore.workitem.entity.WorkItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Represents a runtime instance of a workflow execution for a work item.
 * Workflow instances are pinned to a specific workflow definition version.
 */
@Entity
@Table(name = "workflow_instance")
@Getter
@Setter
public class WorkflowInstance extends BaseEntity {

    @Column(name = "workflow_id", nullable = false, updatable = false)
    private UUID workflowId;

    @Column(name = "workflow_version", nullable = false, updatable = false)
    private Integer workflowVersion;

    @Column(name = "work_item_id", nullable = false, updatable = false)
    private UUID workItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private WorkflowStatus status = WorkflowStatus.NOT_STARTED;

    @Column(name = "started_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Timestamp startedAt;

    @Column(name = "completed_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Timestamp completedAt;

    /**
     * Optional read-only association to parent work item.
     * Not used for persistence, only for convenience queries.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_item_id", insertable = false, updatable = false)
    private WorkItem workItem;

    /**
     * Optional read-only association to step instances.
     * Not used for persistence, only for convenience queries.
     */
    @OneToMany(mappedBy = "workflowInstance", fetch = FetchType.LAZY)
    private List<WorkflowStepInstance> stepInstances;
}
