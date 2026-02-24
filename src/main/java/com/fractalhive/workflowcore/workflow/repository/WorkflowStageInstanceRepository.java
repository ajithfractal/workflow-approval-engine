package com.fractalhive.workflowcore.workflow.repository;

import com.fractalhive.workflowcore.workflow.entity.WorkflowStageInstance;
import com.fractalhive.workflowcore.workflow.enums.StageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for WorkflowStageInstance entities.
 */
@Repository
public interface WorkflowStageInstanceRepository extends JpaRepository<WorkflowStageInstance, UUID> {

    /**
     * Find all stage instances for a workflow instance, ordered by the stage definition's stageOrder.
     *
     * @param workflowInstanceId the workflow instance ID
     * @return list of stage instances
     */
    @Query("SELECT si FROM WorkflowStageInstance si " +
           "JOIN WorkflowStageDefinition sd ON sd.id = si.stageId " +
           "WHERE si.workflowInstanceId = :workflowInstanceId " +
           "ORDER BY sd.stageOrder ASC, si.stageId ASC")
    List<WorkflowStageInstance> findByWorkflowInstanceIdOrderByStageOrderAsc(@Param("workflowInstanceId") UUID workflowInstanceId);

    /**
     * Find stage instances by workflow instance ID and status.
     *
     * @param workflowInstanceId the workflow instance ID
     * @param status the stage status
     * @return list of stage instances
     */
    List<WorkflowStageInstance> findByWorkflowInstanceIdAndStatus(UUID workflowInstanceId, StageStatus status);

    /**
     * Find all stage instances for a workflow instance.
     *
     * @param workflowInstanceId the workflow instance ID
     * @return list of stage instances
     */
    List<WorkflowStageInstance> findByWorkflowInstanceId(UUID workflowInstanceId);
}
