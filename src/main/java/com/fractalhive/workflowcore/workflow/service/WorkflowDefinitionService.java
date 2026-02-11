package com.fractalhive.workflowcore.workflow.service;

import com.fractalhive.workflowcore.workflow.dto.ApproverRequest;
import com.fractalhive.workflowcore.workflow.dto.StepDefinitionRequest;
import com.fractalhive.workflowcore.workflow.dto.WorkflowDefinitionCreateRequest;
import com.fractalhive.workflowcore.workflow.dto.WorkflowDefinitionResponse;

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
     * Adds an approver to a workflow step.
     *
     * @param stepId    the step ID
     * @param request   the approver request
     * @param createdBy the user adding the approver
     * @return the ID of the created approver
     */
    UUID addApprover(UUID stepId, ApproverRequest request, String createdBy);

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
}
