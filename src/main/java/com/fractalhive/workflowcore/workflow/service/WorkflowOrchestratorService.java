package com.fractalhive.workflowcore.workflow.service;

import com.fractalhive.workflowcore.approval.enums.DecisionType;

import java.util.UUID;

/**
 * Service that orchestrates workflow execution.
 * Coordinates between Work Items, Workflow Instances, Step Instances, and Approval Tasks.
 * Provides high-level methods to start workflows and handle approval decisions.
 */
public interface WorkflowOrchestratorService {

    /**
     * Starts a workflow for a work item.
     * Creates workflow instance, step instances, starts the first step, and creates approval tasks.
     *
     * @param workItemId          the work item ID
     * @param workflowDefinitionId the workflow definition ID
     * @param userId              the user starting the workflow
     * @return the created workflow instance ID
     * @throws IllegalArgumentException if work item or workflow definition not found
     */
    UUID startWorkflow(UUID workItemId, UUID workflowDefinitionId, String userId);

    /**
     * Handles an approval decision for a task.
     * Evaluates step completion rules, advances to next step if needed, and completes workflow when all steps are done.
     *
     * @param taskId    the approval task ID
     * @param userId    the user making the decision
     * @param decision  the decision type (APPROVED or REJECTED)
     * @param comments  optional comments
     * @throws IllegalArgumentException if task not found
     * @throws IllegalStateException    if task is not in a valid state for decision
     */
    void handleApprovalDecision(UUID taskId, String userId, DecisionType decision, String comments);

    /**
     * Cancels a workflow instance.
     * Cancels all pending tasks, cancels the workflow instance, and cancels the associated work item.
     *
     * @param workflowInstanceId the workflow instance ID
     * @param userId              the user cancelling the workflow
     * @throws IllegalArgumentException if workflow instance not found
     */
    void cancelWorkflow(UUID workflowInstanceId, String userId);
}
