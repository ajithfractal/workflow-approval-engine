package com.fractalhive.workflowcore.approval.repository;

import com.fractalhive.workflowcore.approval.entity.ApprovalTask;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import com.fractalhive.workflowcore.rulesengine.dto.RuleContextData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ApprovalTask entities.
 */
@Repository
public interface ApprovalTaskRepository extends JpaRepository<ApprovalTask, UUID>, 
        JpaSpecificationExecutor<ApprovalTask> {

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
     * Find all approval tasks for an approver (all statuses), ordered by created date descending.
     *
     * @param approverId the approver ID
     * @return list of approval tasks
     */
    List<ApprovalTask> findByApproverIdOrderByCreatedAtDesc(String approverId);

    /**
     * Optimized query to fetch all rule context data in a single query.
     * Joins task -> step instance -> workflow instance -> work item -> active work item version.
     * Variables (JSONB) are fetched directly - Hibernate will handle conversion.
     * 
     * @param taskId the task ID
     * @return rule context data with variables
     */
    @Query(value = """
        SELECT 
            t.id as taskId,
            t.approver_id as approverId,
            CAST(t.approver_type AS VARCHAR) as approverType,
            CAST(t.status AS VARCHAR) as taskStatus,
            t.due_at as taskDueAt,
            t.acted_at as taskActedAt,
            t.created_at as taskCreatedAt,
            si.id as stepInstanceId,
            si.step_id as stepId,
            CAST(si.status AS VARCHAR) as stepStatus,
            wi.id as workflowInstanceId,
            CAST(wi.status AS VARCHAR) as workflowStatus,
            w.id as workItemId,
            w.type as workItemType,
            CAST(w.status AS VARCHAR) as workItemStatus,
            CAST(wiv.variables AS TEXT) as variables
        FROM approval_task t
        INNER JOIN workflow_step_instance si ON t.step_instance_id = si.id
        INNER JOIN workflow_instance wi ON si.workflow_instance_id = wi.id
        INNER JOIN work_item w ON wi.work_item_id = w.id
        LEFT JOIN work_item_version wiv ON w.id = wiv.work_item_id AND wiv.is_active = true
        WHERE t.id = :taskId
        """, nativeQuery = true)
    Optional<RuleContextData> findRuleContextDataByTaskId(@Param("taskId") UUID taskId);
}
