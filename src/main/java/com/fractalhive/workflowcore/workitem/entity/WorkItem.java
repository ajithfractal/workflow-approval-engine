package com.fractalhive.workflowcore.workitem.entity;

import com.fractalhive.workflowcore.common.entity.BaseEntity;
import com.fractalhive.workflowcore.workitem.enums.WorkItemStatus;
import com.fractalhive.workflowcore.workflow.entity.WorkflowInstance;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Represents a work item (contract, purchase request, etc.) being approved.
 * Work items have versions and lifecycle status.
 */
@Entity
@Table(name = "work_item")
@Getter
@Setter
public class WorkItem extends BaseEntity {

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private WorkItemStatus status = WorkItemStatus.DRAFT;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion = 1;

    /**
     * Content reference (file path, blob reference, etc.) for the latest version.
     * This is a convenience field that mirrors the latest WorkItemVersion's contentRef.
     */
    @Column(name = "content_ref", columnDefinition = "TEXT")
    private String contentRef;

    /**
     * Optional read-only association to versions.
     * Not used for persistence, only for convenience queries.
     */
    @OneToMany(mappedBy = "workItem", fetch = FetchType.LAZY)
    private List<WorkItemVersion> versions;

    /**
     * Optional read-only association to workflow instances.
     * Not used for persistence, only for convenience queries.
     */
    @OneToMany(mappedBy = "workItem", fetch = FetchType.LAZY)
    private List<WorkflowInstance> workflowInstances;
}
