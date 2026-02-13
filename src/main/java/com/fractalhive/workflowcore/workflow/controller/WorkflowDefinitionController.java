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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * REST controller for workflow definition management.
 * Provides endpoints for creating, reading, updating, and deleting workflow definitions,
 * steps, and approvers.
 */
@RestController
@RequestMapping("/api/workflow-definitions")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@Tag(name = "Workflow Definitions", description = "APIs for creating and managing workflow definitions, steps, approvers, and workflow activation")
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
    @Operation(
            summary = "List all workflow definitions",
            description = "Retrieves all workflow definitions in the system"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved workflows",
                    content = @Content(schema = @Schema(implementation = WorkflowDefinitionResponse.class)))
    })
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
    @Operation(
            summary = "Create workflow definition",
            description = "Creates a new workflow definition with a name and version"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Workflow created successfully",
                    content = @Content(schema = @Schema(implementation = CreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or duplicate workflow name+version")
    })
    public ResponseEntity<CreateResponse> createWorkflow(
            @Parameter(description = "Workflow definition creation request")
            @Valid @RequestBody WorkflowDefinitionCreateRequest request,
            @Parameter(description = "User ID creating the workflow", required = true, example = "admin")
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
    @Operation(
            summary = "Get workflow definition by ID",
            description = "Retrieves detailed information about a specific workflow definition including steps and approvers"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Workflow found",
                    content = @Content(schema = @Schema(implementation = WorkflowDefinitionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Workflow not found")
    })
    public ResponseEntity<WorkflowDefinitionResponse> getWorkflow(
            @Parameter(description = "The workflow ID", required = true)
            @PathVariable UUID workflowId) {
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
    @Operation(
            summary = "Update workflow definition",
            description = "Updates a workflow definition. If workflow instances exist, creates a new version instead of updating the existing one"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Workflow updated successfully",
                    content = @Content(schema = @Schema(implementation = CreateResponse.class))),
            @ApiResponse(responseCode = "201", description = "New workflow version created (instances exist for previous version)",
                    content = @Content(schema = @Schema(implementation = CreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Workflow not found")
    })
    public ResponseEntity<CreateResponse> updateWorkflow(
            @Parameter(description = "The workflow ID", required = true)
            @PathVariable UUID workflowId,
            @Parameter(description = "Workflow update request")
            @Valid @RequestBody WorkflowDefinitionCreateRequest request,
            @Parameter(description = "User ID updating the workflow", required = true, example = "admin")
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
    @Operation(
            summary = "Delete workflow definition",
            description = "Deletes a workflow definition. Note: This will fail if workflow instances exist"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Workflow deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot delete workflow with existing instances"),
            @ApiResponse(responseCode = "404", description = "Workflow not found")
    })
    public ResponseEntity<Void> deleteWorkflow(
            @Parameter(description = "The workflow ID", required = true)
            @PathVariable UUID workflowId) {
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
    @Operation(
            summary = "Add step to workflow",
            description = "Adds a new step to a workflow definition. Optionally includes approvers during creation"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Step created successfully",
                    content = @Content(schema = @Schema(implementation = CreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or validation error"),
            @ApiResponse(responseCode = "404", description = "Workflow not found")
    })
    public ResponseEntity<CreateResponse> addStep(
            @Parameter(description = "The workflow ID", required = true)
            @PathVariable UUID workflowId,
            @Parameter(description = "Step definition request with optional approvers")
            @Valid @RequestBody StepDefinitionRequest request,
            @Parameter(description = "User ID creating the step", required = true, example = "admin")
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
    @Operation(
            summary = "Update step definition",
            description = "Updates an existing step definition"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Step updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Step not found")
    })
    public ResponseEntity<Void> updateStep(
            @Parameter(description = "The step ID", required = true)
            @PathVariable UUID stepId,
            @Parameter(description = "Step update request")
            @Valid @RequestBody StepDefinitionRequest request,
            @Parameter(description = "User ID updating the step", required = true, example = "admin")
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
    @Operation(
            summary = "Delete step definition",
            description = "Deletes a step definition from a workflow"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Step deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Step not found")
    })
    public ResponseEntity<Void> deleteStep(
            @Parameter(description = "The step ID", required = true)
            @PathVariable UUID stepId) {
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
    @Operation(
            summary = "Add approvers to step",
            description = "Adds one or more approvers to a workflow step. Validates N_OF_M rule constraints"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Approvers added successfully",
                    content = @Content(schema = @Schema(implementation = ApproversCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or N_OF_M validation failed"),
            @ApiResponse(responseCode = "404", description = "Step not found")
    })
    public ResponseEntity<ApproversCreateResponse> addApprovers(
            @Parameter(description = "The step ID", required = true)
            @PathVariable UUID stepId,
            @Parameter(description = "List of approver requests")
            @Valid @RequestBody List<ApproverRequest> request,
            @Parameter(description = "User ID adding the approvers", required = true, example = "admin")
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
    @Operation(
            summary = "Remove approver from step",
            description = "Removes an approver from a workflow step. Validates N_OF_M rule constraints after removal"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Approver removed successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot remove approver (N_OF_M validation would fail)"),
            @ApiResponse(responseCode = "404", description = "Approver not found")
    })
    public ResponseEntity<Void> removeApprover(
            @Parameter(description = "The approver ID", required = true)
            @PathVariable UUID approverId) {
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
    @Operation(
            summary = "Activate workflow version",
            description = "Activates a workflow version, making it available for use in new workflow instances"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Workflow activated successfully"),
            @ApiResponse(responseCode = "404", description = "Workflow not found")
    })
    public ResponseEntity<Void> activateWorkflow(
            @Parameter(description = "The workflow ID", required = true)
            @PathVariable UUID workflowId,
            @Parameter(description = "User ID activating the workflow", required = true, example = "admin")
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
    @Operation(
            summary = "Deactivate workflow version",
            description = "Deactivates a workflow version, preventing it from being used in new workflow instances"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Workflow deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Workflow not found")
    })
    public ResponseEntity<Void> deactivateWorkflow(
            @Parameter(description = "The workflow ID", required = true)
            @PathVariable UUID workflowId,
            @Parameter(description = "User ID deactivating the workflow", required = true, example = "admin")
            @RequestParam String userId) {
        workflowDefinitionService.deactivateVersion(workflowId, userId);
        return ResponseEntity.noContent().build();
    }
}
