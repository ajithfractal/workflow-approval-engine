package com.fractalhive.workflowcore.workflow.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fractalhive.keycloak.util.SecurityUtils;
import com.fractalhive.workflowcore.workflow.dto.ApproverRequest;
import com.fractalhive.workflowcore.workflow.dto.ApproversCreateResponse;
import com.fractalhive.workflowcore.workflow.dto.CreateResponse;
import com.fractalhive.workflowcore.workflow.dto.StageDefinitionRequest;
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
    public List<WorkflowDefinitionResponse> listWorkflows() {
        return workflowDefinitionService.listWorkflows();
    }

    /**
     * Creates a new workflow definition.
     *
     * @param request   the workflow definition creation request
     * @return the created workflow ID
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create workflow definition",
            description = "Creates a new workflow definition with a name and version. Username is extracted from JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Workflow created successfully",
                    content = @Content(schema = @Schema(implementation = CreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or duplicate workflow name+version")
    })
    public CreateResponse createWorkflow(
            @Parameter(description = "Workflow definition creation request")
            @Valid @RequestBody WorkflowDefinitionCreateRequest request) {
        String createdBy = SecurityUtils.getCurrentUsername();
        UUID workflowId = workflowDefinitionService.createWorkflow(request, createdBy);
        return CreateResponse.builder()
                .id(workflowId)
                .message("Workflow created successfully")
                .build();
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
    public WorkflowDefinitionResponse getWorkflow(
            @Parameter(description = "The workflow ID", required = true)
            @PathVariable UUID workflowId) {
        return workflowDefinitionService.getWorkflow(workflowId);
    }

    /**
     * Updates a workflow definition.
     * If workflow instances exist, creates a new version instead of updating the existing one.
     *
     * @param workflowId the workflow ID
     * @param request     the update request
     * @return the workflow ID (existing if updated, new if versioned)
     */
    @PutMapping("/{workflowId}")
    @Operation(
            summary = "Update workflow definition",
            description = "Updates a workflow definition. If workflow instances exist, creates a new version instead of updating the existing one. Username is extracted from JWT token."
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
            @Valid @RequestBody WorkflowDefinitionCreateRequest request) {
        String updatedBy = SecurityUtils.getCurrentUsername();
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete workflow definition",
            description = "Deletes a workflow definition. Note: This will fail if workflow instances exist"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Workflow deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot delete workflow with existing instances"),
            @ApiResponse(responseCode = "404", description = "Workflow not found")
    })
    public void deleteWorkflow(
            @Parameter(description = "The workflow ID", required = true)
            @PathVariable UUID workflowId) {
        workflowDefinitionService.deleteWorkflow(workflowId);
    }

    /**
     * Creates a stage for a workflow definition.
     *
     * @param workflowId the workflow ID
     * @param request    the stage definition request
     * @return the created stage ID
     */
    @PostMapping("/{workflowId}/stages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create stage for workflow",
            description = "Creates a new stage for a workflow definition. Optionally includes steps during creation. Username is extracted from JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Stage created successfully",
                    content = @Content(schema = @Schema(implementation = CreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or validation error"),
            @ApiResponse(responseCode = "404", description = "Workflow not found")
    })
    public CreateResponse createStage(
            @Parameter(description = "The workflow ID", required = true)
            @PathVariable UUID workflowId,
            @Parameter(description = "Stage definition request with optional steps")
            @Valid @RequestBody StageDefinitionRequest request) {
        String createdBy = SecurityUtils.getCurrentUsername();
        UUID stageId = workflowDefinitionService.createStage(workflowId, request, createdBy);
        return CreateResponse.builder()
                .id(stageId)
                .message("Stage created successfully")
                .build();
    }

    /**
     * Updates a stage definition.
     *
     * @param stageId   the stage ID
     * @param request   the stage update request
     * @return no content
     */
    @PutMapping("/stages/{stageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Update stage definition",
            description = "Updates an existing stage definition. Username is extracted from JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Stage updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Stage not found")
    })
    public void updateStage(
            @Parameter(description = "The stage ID", required = true)
            @PathVariable UUID stageId,
            @Parameter(description = "Stage update request")
            @Valid @RequestBody StageDefinitionRequest request) {
        String updatedBy = SecurityUtils.getCurrentUsername();
        workflowDefinitionService.updateStage(stageId, request, updatedBy);
    }

    /**
     * Deletes a stage definition.
     *
     * @param stageId the stage ID
     * @return no content
     */
    @DeleteMapping("/stages/{stageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete stage definition",
            description = "Deletes a stage definition and all its steps"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Stage deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Stage not found")
    })
    public void deleteStage(
            @Parameter(description = "The stage ID", required = true)
            @PathVariable UUID stageId) {
        workflowDefinitionService.deleteStage(stageId);
    }

    /**
     * Adds a step to a stage.
     *
     * @param stageId   the stage ID
     * @param request   the step definition request
     * @return the created step ID
     */
    @PostMapping("/stages/{stageId}/steps")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Add step to stage",
            description = "Adds a new step to a stage definition. Optionally includes approvers during creation. Username is extracted from JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Step created successfully",
                    content = @Content(schema = @Schema(implementation = CreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or validation error"),
            @ApiResponse(responseCode = "404", description = "Stage not found")
    })
    public CreateResponse addStepToStage(
            @Parameter(description = "The stage ID", required = true)
            @PathVariable UUID stageId,
            @Parameter(description = "Step definition request with optional approvers")
            @Valid @RequestBody StepDefinitionRequest request) {
        String createdBy = SecurityUtils.getCurrentUsername();
        UUID stepId = workflowDefinitionService.addStepToStage(stageId, request, createdBy);
        return CreateResponse.builder()
                .id(stepId)
                .message("Step created successfully")
                .build();
    }

    /**
     * Updates a step definition.
     *
     * @param stepId    the step ID
     * @param request   the step update request
     * @return no content
     */
    @PutMapping("/steps/{stepId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Update step definition",
            description = "Updates an existing step definition. Username is extracted from JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Step updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Step not found")
    })
    public void updateStep(
            @Parameter(description = "The step ID", required = true)
            @PathVariable UUID stepId,
            @Parameter(description = "Step update request")
            @Valid @RequestBody StepDefinitionRequest request) {
        String updatedBy = SecurityUtils.getCurrentUsername();
        workflowDefinitionService.updateStep(stepId, request, updatedBy);
    }

    /**
     * Deletes a step definition.
     *
     * @param stepId the step ID
     * @return no content
     */
    @DeleteMapping("/steps/{stepId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete step definition",
            description = "Deletes a step definition from a workflow"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Step deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Step not found")
    })
    public void deleteStep(
            @Parameter(description = "The step ID", required = true)
            @PathVariable UUID stepId) {
        workflowDefinitionService.deleteStep(stepId);
    }

    /**
     * Adds approvers to a workflow step.
     * Accepts a list of approvers (even if only one).
     *
     * @param stepId    the step ID
     * @param request   wrapper containing list of approver requests
     * @return list of created approver IDs
     */
    @PostMapping("/steps/{stepId}/approvers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Add approvers to step",
            description = "Adds one or more approvers to a workflow step. Validates N_OF_M rule constraints. Username is extracted from JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Approvers added successfully",
                    content = @Content(schema = @Schema(implementation = ApproversCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or N_OF_M validation failed"),
            @ApiResponse(responseCode = "404", description = "Step not found")
    })
    public ApproversCreateResponse addApprovers(
            @Parameter(description = "The step ID", required = true)
            @PathVariable UUID stepId,
            @Parameter(description = "List of approver requests")
            @Valid @RequestBody List<ApproverRequest> request) {
        String createdBy = SecurityUtils.getCurrentUsername();
        List<UUID> approverIds = workflowDefinitionService.addApprovers(stepId, request, createdBy);
        return ApproversCreateResponse.builder()
                .approverIds(approverIds)
                .message(String.format("Successfully added %d approver(s)", approverIds.size()))
                .count(approverIds.size())
                .build();
    }

    /**
     * Removes an approver from a step.
     *
     * @param approverId the approver ID
     * @return no content
     */
    @DeleteMapping("/approvers/{approverId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remove approver from step",
            description = "Removes an approver from a workflow step. Validates N_OF_M rule constraints after removal"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Approver removed successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot remove approver (N_OF_M validation would fail)"),
            @ApiResponse(responseCode = "404", description = "Approver not found")
    })
    public void removeApprover(
            @Parameter(description = "The approver ID", required = true)
            @PathVariable UUID approverId) {
        workflowDefinitionService.removeApprover(approverId);
    }

    /**
     * Activates a workflow version.
     *
     * @param workflowId the workflow ID
     * @return no content
     */
    @PostMapping("/{workflowId}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Activate workflow version",
            description = "Activates a workflow version, making it available for use in new workflow instances. Username is extracted from JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Workflow activated successfully"),
            @ApiResponse(responseCode = "404", description = "Workflow not found")
    })
    public void activateWorkflow(
            @Parameter(description = "The workflow ID", required = true)
            @PathVariable UUID workflowId) {
        String userId = SecurityUtils.getCurrentUsername();
        workflowDefinitionService.activateVersion(workflowId, userId);
    }

    /**
     * Deactivates a workflow version.
     *
     * @param workflowId the workflow ID
     * @return no content
     */
    @PostMapping("/{workflowId}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Deactivate workflow version",
            description = "Deactivates a workflow version, preventing it from being used in new workflow instances. Username is extracted from JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Workflow deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Workflow not found")
    })
    public void deactivateWorkflow(
            @Parameter(description = "The workflow ID", required = true)
            @PathVariable UUID workflowId) {
        String userId = SecurityUtils.getCurrentUsername();
        workflowDefinitionService.deactivateVersion(workflowId, userId);
    }
}
