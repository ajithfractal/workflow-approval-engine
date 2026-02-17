package com.fractalhive.workflowcore.workflow.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fractalhive.workflowcore.rulesengine.dto.RuleContextData;
import com.fractalhive.workflowcore.workflow.dto.RuleContextProjection;
import com.fractalhive.workflowcore.workflow.entity.WorkflowInstance;
import com.fractalhive.workflowcore.workflow.enums.WorkflowStatus;

/**
 * Repository for WorkflowInstance entities.
 */
@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {

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

    /**
     * Find workflow instances by workflow definition ID.
     *
     * @param workflowId the workflow definition ID
     * @return list of workflow instances
     */
    List<WorkflowInstance> findByWorkflowId(UUID workflowId);
    
    
    @Query(value = """
    SELECT 
        wi.id as workflowInstanceId,
        w.id as workItemId,
        wv.version as version,
        wv.is_active as isActive,
        wv.variables as variables
    FROM workflow_instance wi
    JOIN work_item w ON w.id = wi.work_item_id
    LEFT JOIN work_item_version wv 
        ON wv.work_item_id = w.id 
        AND wv.is_active = true
    WHERE wi.id = :id
    ORDER BY wv.version DESC
    LIMIT 1
    """, nativeQuery = true)
    Optional<RuleContextProjection> fetchRuleContext(@Param("id") UUID id);

    /**
     * Optimized query to fetch rule context data for a workflow instance in a single query.
     * Joins workflow instance -> work item -> active work item version.
     * Variables (JSONB) are fetched directly - Hibernate will handle conversion.
     * 
     * @param workflowInstanceId the workflow instance ID
     * @return rule context data with variables
     */
    @Query(value = """
        SELECT 
            wi.id as workflowInstanceId,
            CAST(wi.status AS VARCHAR) as workflowStatus,
            wi.workflow_id as workflowId,
            wi.workflow_version as workflowVersion,
            wi.started_at as workflowStartedAt,
            wi.completed_at as workflowCompletedAt,
            w.id as workItemId,
            w.type as workItemType,
            CAST(w.status AS VARCHAR) as workItemStatus,
            w.current_version as workItemCurrentVersion,
            wiv.content_ref as workItemContentRef,
            CAST(wiv.variables AS TEXT) as variables
        FROM workflow_instance wi
        INNER JOIN work_item w ON wi.work_item_id = w.id
        LEFT JOIN work_item_version wiv ON w.id = wiv.work_item_id AND wiv.is_active = true
        WHERE wi.id = :workflowInstanceId
        """, nativeQuery = true)
    Optional<RuleContextData> findRuleContextDataByWorkflowInstanceId(@Param("workflowInstanceId") UUID workflowInstanceId);
}
