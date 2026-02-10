package com.fractalhive.workflowcore.workflow.entity;

import com.fractalhive.workflowcore.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Represents a workflow definition template.
 * Workflow definitions are versioned and immutable once created.
 */
@Entity
@Table(name = "workflow_definition",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "version"}))
@Getter
@Setter
public class WorkflowDefinition extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Optional read-only association to workflow steps.
     * Not used for persistence, only for convenience queries.
     */
    @OneToMany(mappedBy = "workflowDefinition", fetch = FetchType.LAZY)
    private List<WorkflowStepDefinition> steps;
}
