package com.fractalhive.workflowcore.workflow.entity;

import java.util.List;
import java.util.Map;

import org.hibernate.annotations.Type;

import com.fractalhive.workflowcore.common.entity.BaseEntity;
import com.vladmihalcea.hibernate.type.json.JsonType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

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
     * Optional read-only association to workflow stages.
     * Not used for persistence, only for convenience queries.
     */
    @OneToMany(mappedBy = "workflowDefinition", fetch = FetchType.LAZY)
    private List<WorkflowStageDefinition> stages;
    
    @Column(name = "visual_structure", columnDefinition = "jsonb")
    @Type(JsonType.class)
    private Map<String, Object> visualStructure;
    
}
