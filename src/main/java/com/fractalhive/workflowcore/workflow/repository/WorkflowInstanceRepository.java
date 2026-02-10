package com.fractalhive.workflowcore.workflow.repository;

import com.fractalhive.workflowcore.workflow.entity.WorkflowInstance;
import com.fractalhive.workflowcore.workflow.enums.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WorkflowInstance entities.
 */
@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {

    /**
     * Find workflow instances by work item ID.
     *
     * @param workItemId the work item ID
     * @return list of workflow instances
     */
    List<WorkflowInstance> findByWorkItemId(UUID workItemId);

    /**
     * Find workflow instances by work item ID and status.
     *
     * @param workItemId the work item ID
     * @param statuses   the workflow statuses
     * @return list of workflow instances
     */
    List<WorkflowInstance> findByWorkItemIdAndStatusIn(UUID workItemId, List<WorkflowStatus> statuses);

    /**
     * Find the active workflow instance for a work item.
     *
     * @param workItemId the work item ID
     * @return optional workflow instance
     */
    Optional<WorkflowInstance> findFirstByWorkItemIdAndStatusInOrderByCreatedAtDesc(
            UUID workItemId, List<WorkflowStatus> statuses);
}
