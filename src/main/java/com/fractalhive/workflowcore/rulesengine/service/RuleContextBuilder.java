package com.fractalhive.workflowcore.rulesengine.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fractalhive.workflowcore.approval.enums.ApproverType;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import com.fractalhive.workflowcore.approval.repository.ApprovalTaskRepository;
import com.fractalhive.workflowcore.rulesengine.dto.RuleContext;
import com.fractalhive.workflowcore.rulesengine.dto.RuleContextData;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepInstance;
import com.fractalhive.workflowcore.workflow.enums.StepStatus;
import com.fractalhive.workflowcore.workflow.enums.WorkflowStatus;
import com.fractalhive.workflowcore.workflow.repository.WorkflowInstanceRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStepInstanceRepository;
import com.fractalhive.workflowcore.workitem.enums.WorkItemStatus;

import lombok.RequiredArgsConstructor;

/**
 * Builds RuleContext objects from workflow entities for rule evaluation.
 */
@Service
@RequiredArgsConstructor
public class RuleContextBuilder {

    private static final Logger logger = LoggerFactory.getLogger(RuleContextBuilder.class);

    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowStepInstanceRepository stepInstanceRepository;
    private final ApprovalTaskRepository approvalTaskRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Builds a RuleContext for a workflow instance.
     * @param workflowInstanceId the workflow instance ID
     * @return RuleContext with variables (primary) and optional fields
     */
    public RuleContext buildForWorkflowInstance(UUID workflowInstanceId) {
        // Single optimized query fetches all data in one database call
        RuleContextData data = workflowInstanceRepository
                .findRuleContextDataByWorkflowInstanceId(workflowInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow instance not found: " + workflowInstanceId));

        // Parse variables JSON string to Map (PRIMARY data for rules)
        Map<String, Object> variables = parseVariables(data.getVariables());

        // Build context with variables as primary focus
        return RuleContext.builder()
                .variables(variables)
                .workItemId(data.getWorkItemId())
                .workItemType(data.getWorkItemType())
                .workItemStatus(data.getWorkItemStatus() != null ? 
                        WorkItemStatus.valueOf(data.getWorkItemStatus()) : null)
                .workItemCurrentVersion(data.getWorkItemCurrentVersion())
                .workItemContentRef(data.getWorkItemContentRef())
                .workflowInstanceId(data.getWorkflowInstanceId())
                .workflowId(data.getWorkflowId())
                .workflowVersion(data.getWorkflowVersion())
                .workflowStatus(data.getWorkflowStatus() != null ? 
                        WorkflowStatus.valueOf(data.getWorkflowStatus()) : null)
                .workflowStartedAt(data.getWorkflowStartedAt())
                .workflowCompletedAt(data.getWorkflowCompletedAt())
                .build();
    }

    /**
     * Builds a RuleContext for a step instance.
     * @param stepInstanceId the step instance ID
     * @return RuleContext with variables (primary) and optional fields
     */
    public RuleContext buildForStepInstance(UUID stepInstanceId) {
        // Single optimized query fetches all data in one database call
        RuleContextData data = stepInstanceRepository
                .findRuleContextDataByStepInstanceId(stepInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Step instance not found: " + stepInstanceId));

        // Parse variables JSON string to Map (PRIMARY data for rules)
        Map<String, Object> variables = parseVariables(data.getVariables());

        // Check for parallel steps (only if needed for rules)
        boolean hasParallelSteps = false;
        if (data.getWorkflowInstanceId() != null) {
            List<WorkflowStepInstance> allSteps = stepInstanceRepository
                    .findByWorkflowInstanceId(data.getWorkflowInstanceId());
            hasParallelSteps = allSteps.stream()
                    .anyMatch(s -> !s.getId().equals(stepInstanceId) && 
                            s.getStatus().name().equals(data.getStepStatus()));
        }

        // Build context with variables as primary focus
        return RuleContext.builder()
                .variables(variables) // PRIMARY - this is what rules are written around
                .stepInstanceId(data.getStepInstanceId())
                .stepId(data.getStepId())
                .stepStatus(data.getStepStatus() != null ? 
                        StepStatus.valueOf(data.getStepStatus()) : null)
                .workflowInstanceId(data.getWorkflowInstanceId())
                .workflowStatus(data.getWorkflowStatus() != null ? 
                        WorkflowStatus.valueOf(data.getWorkflowStatus()) : null)
                .workItemId(data.getWorkItemId())
                .workItemType(data.getWorkItemType())
                .workItemStatus(data.getWorkItemStatus() != null ? 
                        WorkItemStatus.valueOf(data.getWorkItemStatus()) : null)
                .hasParallelSteps(hasParallelSteps)
                .build();
    }

    /**
     * Parses variables JSON string to Map.
     * Returns null if variables string is null or empty.
     */
    private Map<String, Object> parseVariables(String variablesJson) {
        if (variablesJson == null || variablesJson.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(variablesJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            logger.warn("Failed to parse variables JSON: {}", variablesJson, e);
            return null;
        }
    }

    /**
     * Builds a RuleContext for an approval task (for SLA evaluation).
     * Uses optimized single-query fetch to avoid N+1 database calls.
     *
     * @param taskId the approval task ID
     * @return RuleContext with variables (primary) and optional fields for SLA/status checks
     */
    public RuleContext buildForTask(UUID taskId) {
        // Single optimized query fetches all data in one database call
        RuleContextData data = approvalTaskRepository
                .findRuleContextDataByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // Parse variables JSON string to Map (PRIMARY data for rules)
        Map<String, Object> variables = parseVariables(data.getVariables());

        // Calculate elapsed hours (for SLA rules)
        Long elapsedHours = null;
        if (data.getTaskCreatedAt() != null) {
            elapsedHours = Duration.between(
                    data.getTaskCreatedAt().toInstant(),
                    Instant.now()
            ).toHours();
        }

        // Calculate hours until due (for SLA rules)
        Long hoursUntilDue = null;
        if (data.getTaskDueAt() != null) {
            hoursUntilDue = Duration.between(
                    Instant.now(),
                    data.getTaskDueAt().toInstant()
            ).toHours();
            if (hoursUntilDue < 0) {
                hoursUntilDue = 0L; // Already past due
            }
        }

        // Build context with variables as primary focus
        return RuleContext.builder()
                .variables(variables) // PRIMARY - this is what rules are written around
                .taskId(data.getTaskId())
                .approverId(data.getApproverId())
                .approverType(data.getApproverType() != null ? 
                        ApproverType.valueOf(data.getApproverType()) : null)
                .taskStatus(data.getTaskStatus() != null ? 
                        TaskStatus.valueOf(data.getTaskStatus()) : null)
                .taskDueAt(data.getTaskDueAt())
                .taskActedAt(data.getTaskActedAt())
                .taskCreatedAt(data.getTaskCreatedAt())
                .stepInstanceId(data.getStepInstanceId())
                .stepId(data.getStepId())
                .stepStatus(data.getStepStatus() != null ? 
                        StepStatus.valueOf(data.getStepStatus()) : null)
                .workflowInstanceId(data.getWorkflowInstanceId())
                .workflowStatus(data.getWorkflowStatus() != null ? 
                        WorkflowStatus.valueOf(data.getWorkflowStatus()) : null)
                .workItemId(data.getWorkItemId())
                .workItemType(data.getWorkItemType())
                .workItemStatus(data.getWorkItemStatus() != null ? 
                        WorkItemStatus.valueOf(data.getWorkItemStatus()) : null)
                .elapsedHours(elapsedHours)
                .hoursUntilDue(hoursUntilDue)
                .build();
    }
}
