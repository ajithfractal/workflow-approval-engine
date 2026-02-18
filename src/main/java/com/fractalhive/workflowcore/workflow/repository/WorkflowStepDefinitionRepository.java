package com.fractalhive.workflowcore.workflow.repository;

import com.fractalhive.workflowcore.workflow.entity.WorkflowStepDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for WorkflowStepDefinition entities.
 */
@Repository
public interface WorkflowStepDefinitionRepository extends JpaRepository<WorkflowStepDefinition, UUID> {

    /**
     * Find all step definitions for a stage, ordered by step order.
     *
     * @param stageId the stage ID
     * @return list of step definitions
     */
    List<WorkflowStepDefinition> findByStageIdOrderByStepOrderAsc(UUID stageId);

    /**
     * Find all step definitions for a stage.
     *
     * @param stageId the stage ID
     * @return list of step definitions
     */
    List<WorkflowStepDefinition> findByStageId(UUID stageId);
}
