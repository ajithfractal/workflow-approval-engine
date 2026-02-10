package com.fractalhive.workflowcore.workflow.repository;

import com.fractalhive.workflowcore.workflow.entity.WorkflowStepInstance;
import com.fractalhive.workflowcore.workflow.enums.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for WorkflowStepInstance entities.
 */
@Repository
public interface WorkflowStepInstanceRepository extends JpaRepository<WorkflowStepInstance, UUID> {

    /**
     * Find all step instances for a workflow instance.
     *
     * @param workflowInstanceId the workflow instance ID
     * @return list of step instances
     */
    List<WorkflowStepInstance> findByWorkflowInstanceId(UUID workflowInstanceId);

    /**
     * Find step instances by workflow instance ID and status.
     *
     * @param workflowInstanceId the workflow instance ID
     * @param status            the step status
     * @return list of step instances
     */
    List<WorkflowStepInstance> findByWorkflowInstanceIdAndStatus(UUID workflowInstanceId, StepStatus status);
}
