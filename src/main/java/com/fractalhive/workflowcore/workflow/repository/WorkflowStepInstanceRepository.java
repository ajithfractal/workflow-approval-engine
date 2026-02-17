package com.fractalhive.workflowcore.workflow.repository;

import com.fractalhive.workflowcore.rulesengine.dto.RuleContextData;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepInstance;
import com.fractalhive.workflowcore.workflow.enums.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
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

    /**
     * Optimized query to fetch rule context data for a step instance in a single query.
     * Joins step instance -> workflow instance -> work item -> active work item version.
     * Variables (JSONB) are fetched directly - Hibernate will handle conversion.
     * 
     * @param stepInstanceId the step instance ID
     * @return rule context data with variables
     */
    @Query(value = """
        SELECT 
            si.id as stepInstanceId,
            si.step_id as stepId,
            CAST(si.status AS VARCHAR) as stepStatus,
            wi.id as workflowInstanceId,
            CAST(wi.status AS VARCHAR) as workflowStatus,
            w.id as workItemId,
            w.type as workItemType,
            CAST(w.status AS VARCHAR) as workItemStatus,
            CAST(wiv.variables AS TEXT) as variables
        FROM workflow_step_instance si
        INNER JOIN workflow_instance wi ON si.workflow_instance_id = wi.id
        INNER JOIN work_item w ON wi.work_item_id = w.id
        LEFT JOIN work_item_version wiv ON w.id = wiv.work_item_id AND wiv.is_active = true
        WHERE si.id = :stepInstanceId
        """, nativeQuery = true)
    Optional<RuleContextData> findRuleContextDataByStepInstanceId(@Param("stepInstanceId") UUID stepInstanceId);
}
