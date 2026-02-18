package com.fractalhive.workflowcore.workflow.repository;

import com.fractalhive.workflowcore.workflow.entity.WorkflowStageDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WorkflowStageDefinition entities.
 */
@Repository
public interface WorkflowStageDefinitionRepository extends JpaRepository<WorkflowStageDefinition, UUID> {

    /**
     * Find all stage definitions for a workflow, ordered by stage order.
     * Uses JOIN FETCH to load steps in the same query to avoid N+1 problem.
     *
     * @param workflowId the workflow ID
     * @return list of stage definitions with steps loaded
     */
    @Query("SELECT DISTINCT s FROM WorkflowStageDefinition s " +
           "LEFT JOIN FETCH s.steps " +
           "WHERE s.workflowId = :workflowId " +
           "ORDER BY s.stageOrder ASC")
    List<WorkflowStageDefinition> findByWorkflowIdOrderByStageOrderAsc(@Param("workflowId") UUID workflowId);

    /**
     * Find stage definition by ID with steps loaded.
     * Uses JOIN FETCH to load steps in the same query.
     *
     * @param id the stage definition ID
     * @return optional stage definition with steps loaded
     */
    @Query("SELECT DISTINCT s FROM WorkflowStageDefinition s " +
           "LEFT JOIN FETCH s.steps " +
           "WHERE s.id = :id")
    Optional<WorkflowStageDefinition> findByIdWithSteps(@Param("id") UUID id);
}
