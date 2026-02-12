package com.fractalhive.workflowcore.workflow.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fractalhive.workflowcore.workflow.dto.ApproverRequest;
import com.fractalhive.workflowcore.workflow.dto.ApproversCreateResponse;
import com.fractalhive.workflowcore.workflow.dto.CreateResponse;
import com.fractalhive.workflowcore.workflow.dto.StepDefinitionRequest;
import com.fractalhive.workflowcore.workflow.dto.WorkflowDefinitionCreateRequest;
import com.fractalhive.workflowcore.workflow.dto.WorkflowDefinitionResponse;
import com.fractalhive.workflowcore.workflow.service.WorkflowDefinitionService;

import jakarta.validation.Valid;

/**
 * REST controller for workflow definition management.
 * Provides endpoints for creating, reading, updating, and deleting workflow definitions,
 * steps, and approvers.
 */
@RestController
@RequestMapping("/api/workflow-definitions")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class WorkflowDefinitionController {

    private final WorkflowDefinitionService workflowDefinitionService;

    public WorkflowDefinitionController(WorkflowDefinitionService workflowDefinitionService) {
        this.workflowDefinitionService = workflowDefinitionService;
    }

    /**
     * Lists all workflow definitions.
     *
     * @return list of workflow definitions
     */
    @GetMapping
    public ResponseEntity<List<WorkflowDefinitionResponse>> listWorkflows() {
        List<WorkflowDefinitionResponse> workflows = workflowDefinitionService.listWorkflows();
        return ResponseEntity.ok(workflows);
    }

    /**
     * Creates a new workflow definition.
     *
     * @param request   the workflow definition creation request
     * @param createdBy the user creating the workflow
     * @return the created workflow ID
     */
    @PostMapping
    public ResponseEntity<CreateResponse> createWorkflow(
            @Valid @RequestBody WorkflowDefinitionCreateRequest request,
            @RequestParam String createdBy) {
        UUID workflowId = workflowDefinitionService.createWorkflow(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CreateResponse.builder()
                        .id(workflowId)
                        .message("Workflow created successfully")
                        .build());
    }

    /**
     * Gets a workflow definition by ID.
     *
     * @param workflowId the workflow ID
     * @return the workflow definition
     */
    @GetMapping("/{workflowId}")
    public ResponseEntity<WorkflowDefinitionResponse> getWorkflow(@PathVariable UUID workflowId) {
        WorkflowDefinitionResponse workflow = workflowDefinitionService.getWorkflow(workflowId);
        return ResponseEntity.ok(workflow);
    }

    /**
     * Updates a workflow definition.
     * If workflow instances exist, creates a new version instead of updating the existing one.
     *
     * @param workflowId the workflow ID
     * @param request     the update request
     * @param updatedBy   the user updating the workflow
     * @return the workflow ID (existing if updated, new if versioned)
     */
    @PutMapping("/{workflowId}")
    public ResponseEntity<CreateResponse> updateWorkflow(
            @PathVariable UUID workflowId,
            @Valid @RequestBody WorkflowDefinitionCreateRequest request,
            @RequestParam String updatedBy) {
        UUID resultWorkflowId = workflowDefinitionService.updateWorkflow(workflowId, request, updatedBy);
        
        if (resultWorkflowId.equals(workflowId)) {
            // Existing workflow was updated
            return ResponseEntity.ok(CreateResponse.builder()
                    .id(resultWorkflowId)
                    .message("Workflow updated successfully")
                    .build());
        } else {
            // New version was created
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CreateResponse.builder()
                            .id(resultWorkflowId)
                            .message("New workflow version created successfully (instances exist for previous version)")
                            .build());
        }
    }

    /**
     * Deletes a workflow definition.
     *
     * @param workflowId the workflow ID
     * @return no content
     */
    @DeleteMapping("/{workflowId}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable UUID workflowId) {
        workflowDefinitionService.deleteWorkflow(workflowId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds a step to a workflow definition.
     *
     * @param workflowId the workflow ID
     * @param request    the step definition request
     * @param createdBy  the user creating the step
     * @return the created step ID
     */
    @PostMapping("/{workflowId}/steps")
    public ResponseEntity<CreateResponse> addStep(
            @PathVariable UUID workflowId,
            @Valid @RequestBody StepDefinitionRequest request,
            @RequestParam String createdBy) {
        UUID stepId = workflowDefinitionService.createStep(workflowId, request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CreateResponse.builder()
                        .id(stepId)
                        .message("Step created successfully")
                        .build());
    }

    /**
     * Updates a step definition.
     *
     * @param stepId    the step ID
     * @param request   the step update request
     * @param updatedBy the user updating the step
     * @return no content
     */
    @PutMapping("/steps/{stepId}")
    public ResponseEntity<Void> updateStep(
            @PathVariable UUID stepId,
            @Valid @RequestBody StepDefinitionRequest request,
            @RequestParam String updatedBy) {
        workflowDefinitionService.updateStep(stepId, request, updatedBy);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes a step definition.
     *
     * @param stepId the step ID
     * @return no content
     */
    @DeleteMapping("/steps/{stepId}")
    public ResponseEntity<Void> deleteStep(@PathVariable UUID stepId) {
        workflowDefinitionService.deleteStep(stepId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds approvers to a workflow step.
     * Accepts a list of approvers (even if only one).
     *
     * @param stepId    the step ID
     * @param request   wrapper containing list of approver requests
     * @param createdBy the user adding the approvers
     * @return list of created approver IDs
     */
    @PostMapping("/steps/{stepId}/approvers")
    public ResponseEntity<ApproversCreateResponse> addApprovers(
            @PathVariable UUID stepId,
            @Valid @RequestBody List<ApproverRequest> request,
            @RequestParam String createdBy) {
        List<UUID> approverIds = workflowDefinitionService.addApprovers(stepId, request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApproversCreateResponse.builder()
                        .approverIds(approverIds)
                        .message(String.format("Successfully added %d approver(s)", approverIds.size()))
                        .count(approverIds.size())
                        .build());
    }

    /**
     * Removes an approver from a step.
     *
     * @param approverId the approver ID
     * @return no content
     */
    @DeleteMapping("/approvers/{approverId}")
    public ResponseEntity<Void> removeApprover(@PathVariable UUID approverId) {
        workflowDefinitionService.removeApprover(approverId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activates a workflow version.
     *
     * @param workflowId the workflow ID
     * @param userId     the user activating the workflow
     * @return no content
     */
    @PostMapping("/{workflowId}/activate")
    public ResponseEntity<Void> activateWorkflow(
            @PathVariable UUID workflowId,
            @RequestParam String userId) {
        workflowDefinitionService.activateVersion(workflowId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deactivates a workflow version.
     *
     * @param workflowId the workflow ID
     * @param userId      the user deactivating the workflow
     * @return no content
     */
    @PostMapping("/{workflowId}/deactivate")
    public ResponseEntity<Void> deactivateWorkflow(
            @PathVariable UUID workflowId,
            @RequestParam String userId) {
        workflowDefinitionService.deactivateVersion(workflowId, userId);
        return ResponseEntity.noContent().build();
    }
}
