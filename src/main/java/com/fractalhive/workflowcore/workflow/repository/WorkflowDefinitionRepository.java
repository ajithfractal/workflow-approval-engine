package com.fractalhive.workflowcore.workflow.repository;

import com.fractalhive.workflowcore.workflow.entity.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WorkflowDefinition entities.
 */
@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {

    /**
     * Find workflow definitions by name, active status, ordered by version descending.
     *
     * @param name the workflow name
     * @return list of workflow definitions
     */
    List<WorkflowDefinition> findByNameAndIsActiveTrueOrderByVersionDesc(String name);

    /**
     * Find the latest active version of a workflow by name.
     *
     * @param name the workflow name
     * @return optional workflow definition
     */
    Optional<WorkflowDefinition> findFirstByNameAndIsActiveTrueOrderByVersionDesc(String name);

    /**
     * Find workflow definition by name and version.
     *
     * @param name    the workflow name
     * @param version the workflow version
     * @return optional workflow definition
     */
    Optional<WorkflowDefinition> findByNameAndVersion(String name, Integer version);

    /**
     * Find the latest version of a workflow by name (highest version number).
     *
     * @param name the workflow name
     * @return optional workflow definition with highest version
     */
    Optional<WorkflowDefinition> findFirstByNameOrderByVersionDesc(String name);
}
