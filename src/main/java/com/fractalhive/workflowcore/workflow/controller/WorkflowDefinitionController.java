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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fractalhive.keycloak.util.SecurityUtils;
import com.fractalhive.workflowcore.workflow.dto.ApproverRequest;

import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowDefinition.*;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Common.*;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ResponseCodes.*;
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

    @GetMapping
    @Operation(
            summary = SUMMARY_LIST_ALL,
            description = DESC_LIST_ALL
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_RETRIEVED_WORKFLOWS,
                    content = @Content(schema = @Schema(implementation = WorkflowDefinitionResponse.class)))
    })
    public List<WorkflowDefinitionResponse> listWorkflows() {
        return workflowDefinitionService.listWorkflows();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = SUMMARY_CREATE,
            description = DESC_CREATE
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = CREATED_201, description = RESP_CREATED_SUCCESS,
                    content = @Content(schema = @Schema(implementation = CreateResponse.class))),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = RESP_INVALID_OR_DUPLICATE)
    })
    public CreateResponse createWorkflow(
            @Parameter(description = PARAM_CREATE_REQUEST)
            @Valid @RequestBody WorkflowDefinitionCreateRequest request) {
        String createdBy = SecurityUtils.getCurrentUsername();
        UUID workflowId = workflowDefinitionService.createWorkflow(request, createdBy);
        return CreateResponse.builder()
                .id(workflowId)
                .message("Workflow created successfully")
                .build();
    }

    @GetMapping("/{workflowId}")
    @Operation(
            summary = SUMMARY_GET_BY_ID,
            description = DESC_GET_BY_ID
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_WORKFLOW_FOUND,
                    content = @Content(schema = @Schema(implementation = WorkflowDefinitionResponse.class))),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public WorkflowDefinitionResponse getWorkflow(
            @Parameter(description = PARAM_WORKFLOW_ID, required = true)
            @PathVariable UUID workflowId) {
        return workflowDefinitionService.getWorkflow(workflowId);
    }

    @PutMapping("/{workflowId}")
    @Operation(
            summary = SUMMARY_UPDATE,
            description = DESC_UPDATE
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_UPDATED_SUCCESS,
                    content = @Content(schema = @Schema(implementation = CreateResponse.class))),
            @ApiResponse(responseCode = CREATED_201, description = RESP_VERSION_CREATED,
                    content = @Content(schema = @Schema(implementation = CreateResponse.class))),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = INVALID_REQUEST),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public ResponseEntity<CreateResponse> updateWorkflow(
            @Parameter(description = PARAM_WORKFLOW_ID, required = true)
            @PathVariable UUID workflowId,
            @Parameter(description = PARAM_UPDATE_REQUEST)
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

    @DeleteMapping("/{workflowId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = SUMMARY_DELETE,
            description = DESC_DELETE
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = NO_CONTENT_204, description = RESP_DELETED_SUCCESS),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = RESP_CANNOT_DELETE_WITH_INSTANCES),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public void deleteWorkflow(
            @Parameter(description = PARAM_WORKFLOW_ID, required = true)
            @PathVariable UUID workflowId) {
        workflowDefinitionService.deleteWorkflow(workflowId);
    }

    @PostMapping("/{workflowId}/stages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = SUMMARY_CREATE_STAGE,
            description = DESC_CREATE_STAGE
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = CREATED_201, description = RESP_STAGE_CREATED,
                    content = @Content(schema = @Schema(implementation = CreateResponse.class))),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = INVALID_REQUEST),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public CreateResponse createStage(
            @Parameter(description = PARAM_WORKFLOW_ID, required = true)
            @PathVariable UUID workflowId,
            @Parameter(description = PARAM_STAGE_REQUEST)
            @Valid @RequestBody StageDefinitionRequest request) {
        String createdBy = SecurityUtils.getCurrentUsername();
        UUID stageId = workflowDefinitionService.createStage(workflowId, request, createdBy);
        return CreateResponse.builder()
                .id(stageId)
                .message("Stage created successfully")
                .build();
    }

    @PutMapping("/stages/{stageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = SUMMARY_UPDATE_STAGE,
            description = DESC_UPDATE_STAGE
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = NO_CONTENT_204, description = RESP_STAGE_UPDATED),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = INVALID_REQUEST),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public void updateStage(
            @Parameter(description = PARAM_STAGE_ID, required = true)
            @PathVariable UUID stageId,
            @Parameter(description = PARAM_STAGE_REQUEST)
            @Valid @RequestBody StageDefinitionRequest request) {
        String updatedBy = SecurityUtils.getCurrentUsername();
        workflowDefinitionService.updateStage(stageId, request, updatedBy);
    }

    @DeleteMapping("/stages/{stageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = SUMMARY_DELETE_STAGE,
            description = DESC_DELETE_STAGE
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = NO_CONTENT_204, description = RESP_STAGE_DELETED),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public void deleteStage(
            @Parameter(description = PARAM_STAGE_ID, required = true)
            @PathVariable UUID stageId) {
        workflowDefinitionService.deleteStage(stageId);
    }

    @PostMapping("/stages/{stageId}/steps")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = SUMMARY_ADD_STEP,
            description = DESC_ADD_STEP
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = CREATED_201, description = RESP_STEP_CREATED,
                    content = @Content(schema = @Schema(implementation = CreateResponse.class))),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = INVALID_REQUEST),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public CreateResponse addStepToStage(
            @Parameter(description = PARAM_STAGE_ID, required = true)
            @PathVariable UUID stageId,
            @Parameter(description = PARAM_STEP_REQUEST)
            @Valid @RequestBody StepDefinitionRequest request) {
        String createdBy = SecurityUtils.getCurrentUsername();
        UUID stepId = workflowDefinitionService.addStepToStage(stageId, request, createdBy);
        return CreateResponse.builder()
                .id(stepId)
                .message("Step created successfully")
                .build();
    }

    @PutMapping("/steps/{stepId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = SUMMARY_UPDATE_STEP,
            description = DESC_UPDATE_STEP
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = NO_CONTENT_204, description = RESP_STEP_UPDATED),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = INVALID_REQUEST),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public void updateStep(
            @Parameter(description = PARAM_STEP_ID, required = true)
            @PathVariable UUID stepId,
            @Parameter(description = PARAM_STEP_REQUEST)
            @Valid @RequestBody StepDefinitionRequest request) {
        String updatedBy = SecurityUtils.getCurrentUsername();
        workflowDefinitionService.updateStep(stepId, request, updatedBy);
    }

    @DeleteMapping("/steps/{stepId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = SUMMARY_DELETE_STEP,
            description = DESC_DELETE_STEP
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = NO_CONTENT_204, description = RESP_STEP_DELETED),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public void deleteStep(
            @Parameter(description = PARAM_STEP_ID, required = true)
            @PathVariable UUID stepId) {
        workflowDefinitionService.deleteStep(stepId);
    }

    @PostMapping("/steps/{stepId}/approvers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = SUMMARY_ADD_APPROVERS,
            description = DESC_ADD_APPROVERS
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = CREATED_201, description = RESP_APPROVERS_ADDED,
                    content = @Content(schema = @Schema(implementation = ApproversCreateResponse.class))),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = RESP_N_OF_M_VALIDATION_FAILED),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public ApproversCreateResponse addApprovers(
            @Parameter(description = PARAM_STEP_ID, required = true)
            @PathVariable UUID stepId,
            @Parameter(description = PARAM_APPROVER_REQUEST)
            @Valid @RequestBody List<ApproverRequest> request) {
        String createdBy = SecurityUtils.getCurrentUsername();
        List<UUID> approverIds = workflowDefinitionService.addApprovers(stepId, request, createdBy);
        return ApproversCreateResponse.builder()
                .approverIds(approverIds)
                .message(String.format("Successfully added %d approver(s)", approverIds.size()))
                .count(approverIds.size())
                .build();
    }

    @DeleteMapping("/approvers/{approverId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = SUMMARY_REMOVE_APPROVER,
            description = DESC_REMOVE_APPROVER
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = NO_CONTENT_204, description = RESP_APPROVER_REMOVED),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = RESP_CANNOT_REMOVE_APPROVER),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public void removeApprover(
            @Parameter(description = PARAM_APPROVER_ID, required = true)
            @PathVariable UUID approverId) {
        workflowDefinitionService.removeApprover(approverId);
    }

    @PostMapping("/{workflowId}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = SUMMARY_ACTIVATE,
            description = DESC_ACTIVATE
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = NO_CONTENT_204, description = RESP_WORKFLOW_ACTIVATED),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public void activateWorkflow(
            @Parameter(description = PARAM_WORKFLOW_ID, required = true)
            @PathVariable UUID workflowId) {
        String userId = SecurityUtils.getCurrentUsername();
        workflowDefinitionService.activateVersion(workflowId, userId);
    }

    @PostMapping("/{workflowId}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = SUMMARY_DEACTIVATE,
            description = DESC_DEACTIVATE
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = NO_CONTENT_204, description = RESP_WORKFLOW_DEACTIVATED),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public void deactivateWorkflow(
            @Parameter(description = PARAM_WORKFLOW_ID, required = true)
            @PathVariable UUID workflowId) {
        String userId = SecurityUtils.getCurrentUsername();
        workflowDefinitionService.deactivateVersion(workflowId, userId);
    }
}
