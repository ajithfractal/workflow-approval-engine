package com.fractalhive.workflowcore.workitem.entity;

import com.fractalhive.workflowcore.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Represents a version of a work item.
 * Each submission or rework creates a new version with immutable content reference.
 */
@Entity
@Table(name = "work_item_version",
       uniqueConstraints = @UniqueConstraint(columnNames = {"work_item_id", "version"}))
@Getter
@Setter
public class WorkItemVersion extends BaseEntity {

    @Column(name = "work_item_id", nullable = false, updatable = false)
    private UUID workItemId;

    @Column(name = "version", nullable = false, updatable = false)
    private Integer version;

    @Column(name = "content_ref", columnDefinition = "TEXT")
    private String contentRef;

    @Column(name = "submitted_by", length = 50)
    private String submittedBy;

    @Column(name = "submitted_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Timestamp submittedAt;

    /**
     * Optional read-only association to parent work item.
     * Not used for persistence, only for convenience queries.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_item_id", insertable = false, updatable = false)
    private WorkItem workItem;
}
