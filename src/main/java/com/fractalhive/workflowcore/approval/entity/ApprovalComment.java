package com.fractalhive.workflowcore.approval.entity;

import com.fractalhive.workflowcore.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Represents an additional comment or feedback on an approval task.
 * Multiple comments can be added to a single approval task.
 */
@Entity
@Table(name = "approval_comment")
@Getter
@Setter
public class ApprovalComment extends BaseEntity {

    @Column(name = "approval_task_id", nullable = false, updatable = false)
    private UUID approvalTaskId;

    @Column(name = "comment", nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Column(name = "commented_by", nullable = false, length = 100)
    private String commentedBy;

    @Column(name = "commented_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Timestamp commentedAt;

    /**
     * Optional read-only association to parent approval task.
     * Not used for persistence, only for convenience queries.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_task_id", insertable = false, updatable = false)
    private ApprovalTask approvalTask;
}
