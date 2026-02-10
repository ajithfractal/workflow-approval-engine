package com.fractalhive.workflowcore.approval.repository;

import com.fractalhive.workflowcore.approval.entity.ApprovalTask;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ApprovalTask entities.
 */
@Repository
public interface ApprovalTaskRepository extends JpaRepository<ApprovalTask, UUID> {

    /**
     * Find all approval tasks for a step instance.
     *
     * @param stepInstanceId the step instance ID
     * @return list of approval tasks
     */
    List<ApprovalTask> findByStepInstanceId(UUID stepInstanceId);

    /**
     * Find approval tasks by step instance ID and status.
     *
     * @param stepInstanceId the step instance ID
     * @param status         the task status
     * @return list of approval tasks
     */
    List<ApprovalTask> findByStepInstanceIdAndStatus(UUID stepInstanceId, TaskStatus status);

    /**
     * Find approval tasks by approver ID and status.
     *
     * @param approverId the approver ID
     * @param status     the task status
     * @return list of approval tasks
     */
    List<ApprovalTask> findByApproverIdAndStatus(String approverId, TaskStatus status);

    /**
     * Find all pending approval tasks for an approver.
     *
     * @param approverId the approver ID
     * @return list of pending approval tasks
     */
    List<ApprovalTask> findByApproverIdAndStatusOrderByDueAtAsc(String approverId, TaskStatus status);
}
