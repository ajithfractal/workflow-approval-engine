package com.fractalhive.workflowcore.workflow.service;

import com.fractalhive.workflowcore.workflow.dto.ApproverRequest;
import com.fractalhive.workflowcore.workflow.dto.StepDefinitionRequest;
import com.fractalhive.workflowcore.workflow.dto.WorkflowDefinitionCreateRequest;
import com.fractalhive.workflowcore.workflow.dto.WorkflowDefinitionResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing workflow definitions.
 */
public interface WorkflowDefinitionService {

    /**
     * Creates a new workflow definition.
     *
     * @param request   the workflow definition creation request
     * @param createdBy the user creating the workflow
     * @return the ID of the created workflow definition
     */
    UUID createWorkflow(WorkflowDefinitionCreateRequest request, String createdBy);

    /**
     * Creates a step definition for a workflow.
     *
     * @param workflowId the workflow ID
     * @param request    the step definition request
     * @param createdBy  the user creating the step
     * @return the ID of the created step definition
     */
    UUID createStep(UUID workflowId, StepDefinitionRequest request, String createdBy);

    /**
     * Adds approvers to a workflow step.
     * Accepts a list of approvers (even if only one).
     * For N_OF_M approval type, validates that minApprovals doesn't exceed total approvers.
     *
     * @param stepId    the step ID
     * @param requests  list of approver requests
     * @param createdBy the user adding the approvers
     * @return list of created approver IDs
     */
    List<UUID> addApprovers(UUID stepId, List<ApproverRequest> requests, String createdBy);

    /**
     * Retrieves a workflow definition by ID.
     *
     * @param workflowId the workflow ID
     * @return the workflow definition response
     */
    WorkflowDefinitionResponse getWorkflow(UUID workflowId);

    /**
     * Retrieves a workflow definition by name and version.
     *
     * @param name    the workflow name
     * @param version the workflow version
     * @return the workflow definition response
     */
    WorkflowDefinitionResponse getWorkflowByNameAndVersion(String name, Integer version);

    /**
     * Retrieves the latest active workflow definition by name.
     *
     * @param name the workflow name
     * @return the workflow definition response
     */
    WorkflowDefinitionResponse getLatestActiveWorkflow(String name);

    /**
     * Activates a workflow version.
     *
     * @param workflowId the workflow ID
     * @param userId     the user activating the version
     */
    void activateVersion(UUID workflowId, String userId);

    /**
     * Deactivates a workflow version.
     *
     * @param workflowId the workflow ID
     * @param userId     the user deactivating the version
     */
    void deactivateVersion(UUID workflowId, String userId);

    /**
     * Lists all workflow definitions.
     *
     * @return list of workflow definitions
     */
    List<WorkflowDefinitionResponse> listWorkflows();

    /**
     * Updates a workflow definition.
     * If workflow instances exist, creates a new version instead of updating the existing one.
     *
     * @param workflowId the workflow ID
     * @param request    the update request
     * @param updatedBy  the user updating the workflow
     * @return the workflow ID (existing if updated, new if versioned)
     */
    UUID updateWorkflow(UUID workflowId, WorkflowDefinitionCreateRequest request, String updatedBy);

    /**
     * Deletes a workflow definition.
     *
     * @param workflowId the workflow ID
     */
    void deleteWorkflow(UUID workflowId);

    /**
     * Updates a step definition.
     *
     * @param stepId    the step ID
     * @param request   the step update request
     * @param updatedBy the user updating the step
     */
    void updateStep(UUID stepId, StepDefinitionRequest request, String updatedBy);

    /**
     * Deletes a step definition.
     *
     * @param stepId the step ID
     */
    void deleteStep(UUID stepId);

    /**
     * Removes an approver from a step.
     *
     * @param approverId the approver ID
     */
    void removeApprover(UUID approverId);
}
