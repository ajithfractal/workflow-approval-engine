package com.fractalhive.workflowcore.workitem.controller;

import com.fractalhive.keycloak.util.SecurityUtils;
import com.fractalhive.workflowcore.workitem.dto.WorkItemCreateRequest;

import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkItem.*;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Common.*;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ResponseCodes.*;
import com.fractalhive.workflowcore.workitem.dto.WorkItemResponse;
import com.fractalhive.workflowcore.workitem.dto.WorkItemSubmitRequest;
import com.fractalhive.workflowcore.workitem.dto.WorkItemVersionResponse;
import com.fractalhive.workflowcore.workitem.dto.WorkflowProgressResponse;
import com.fractalhive.workflowcore.workitem.enums.WorkItemStatus;
import com.fractalhive.workflowcore.workitem.service.WorkItemService;
import com.fractalhive.workflowcore.workitem.statemachine.service.WorkItemStateMachineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for work item operations.
 */
@RestController
@RequestMapping("/api/work-items")
@Tag(name = "Work Items", description = "APIs for managing work items, their versions, workflow progress, and archiving")
public class WorkItemController {

    private final WorkItemService workItemService;
    private final WorkItemStateMachineService workItemStateMachineService;

    public WorkItemController(WorkItemService workItemService,
                              WorkItemStateMachineService workItemStateMachineService) {
        this.workItemService = workItemService;
        this.workItemStateMachineService = workItemStateMachineService;
    }

    @GetMapping
    @Operation(
            summary = SUMMARY_LIST,
            description = DESC_LIST
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_RETRIEVED_WORK_ITEMS,
                    content = @Content(schema = @Schema(implementation = WorkItemResponse.class)))
    })
    public List<WorkItemResponse> listWorkItems(
            @Parameter(description = PARAM_STATUS_FILTER, example = "DRAFT")
            @RequestParam(required = false) WorkItemStatus status,
            @Parameter(description = PARAM_TYPE_FILTER, example = "contract")
            @RequestParam(required = false) String type) {
        return workItemService.listWorkItems(status, type);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = SUMMARY_CREATE,
            description = DESC_CREATE
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = CREATED_201, description = RESP_CREATED_SUCCESS,
                    content = @Content(schema = @Schema(implementation = WorkItemCreateResponse.class))),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = INVALID_REQUEST)
    })
    public WorkItemCreateResponse createWorkItem(
            @Parameter(description = PARAM_CREATE_REQUEST)
            @Valid @RequestBody WorkItemCreateRequest request) {
        String createdBy = SecurityUtils.getCurrentUsername();
        UUID workItemId = workItemService.createWorkItem(request, createdBy);
        return WorkItemCreateResponse.builder().workItemId(workItemId).build();
    }

    @GetMapping("/{workItemId}")
    @Operation(
            summary = SUMMARY_GET_BY_ID,
            description = DESC_GET_BY_ID
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_FOUND,
                    content = @Content(schema = @Schema(implementation = WorkItemResponse.class))),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public WorkItemResponse getWorkItem(
            @Parameter(description = PARAM_WORK_ITEM_ID, required = true)
            @PathVariable UUID workItemId) {
        return workItemService.getWorkItem(workItemId);
    }

    @PostMapping("/{workItemId}/submit")
    @Operation(
            summary = SUMMARY_SUBMIT,
            description = DESC_SUBMIT
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_SUBMITTED_SUCCESS,
                    content = @Content(schema = @Schema(implementation = WorkItemSubmitResponse.class))),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = RESP_CANNOT_SUBMIT),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public WorkItemSubmitResponse submitWorkItem(
            @Parameter(description = PARAM_WORK_ITEM_ID, required = true)
            @PathVariable UUID workItemId,
            @Parameter(description = PARAM_SUBMIT_REQUEST)
            @Valid @RequestBody WorkItemSubmitRequest request) {
        String submittedBy = SecurityUtils.getCurrentUsername();
        UUID versionId = workItemService.submitWorkItem(workItemId, request, submittedBy);
        return WorkItemSubmitResponse.builder().versionId(versionId).build();
    }

    @PostMapping("/submit")
    @Operation(
            summary = SUMMARY_SUBMIT_CONVENIENCE,
            description = DESC_SUBMIT_CONVENIENCE
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_SUBMITTED_SUCCESS,
                    content = @Content(schema = @Schema(implementation = WorkItemSubmitResponse.class))),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = RESP_CANNOT_SUBMIT),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public WorkItemSubmitResponse submitWorkItemConvenience(
            @Parameter(description = PARAM_SUBMIT_CONVENIENCE_REQUEST)
            @Valid @RequestBody WorkItemSubmitRequest request) {
        String submittedBy = SecurityUtils.getCurrentUsername();
        UUID versionId = workItemService.submitWorkItem(null, request, submittedBy);
        return WorkItemSubmitResponse.builder().versionId(versionId).build();
    }

    @GetMapping("/{workItemId}/versions")
    @Operation(
            summary = SUMMARY_GET_VERSIONS,
            description = DESC_GET_VERSIONS
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_RETRIEVED_VERSIONS,
                    content = @Content(schema = @Schema(implementation = WorkItemVersionResponse.class))),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public List<WorkItemVersionResponse> getVersions(
            @Parameter(description = PARAM_WORK_ITEM_ID, required = true)
            @PathVariable UUID workItemId) {
        return workItemService.getVersions(workItemId);
    }

    @GetMapping("/{workItemId}/workflow-progress")
    @Operation(
            summary = SUMMARY_GET_PROGRESS,
            description = DESC_GET_PROGRESS
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_RETRIEVED_PROGRESS,
                    content = @Content(schema = @Schema(implementation = WorkflowProgressResponse.class))),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public WorkflowProgressResponse getWorkflowProgress(
            @Parameter(description = PARAM_WORK_ITEM_ID, required = true)
            @PathVariable UUID workItemId) {
        return workItemService.getWorkflowProgress(workItemId);
    }

    @PostMapping("/{workItemId}/archive")
    @Operation(
            summary = SUMMARY_ARCHIVE,
            description = DESC_ARCHIVE
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_ARCHIVED_SUCCESS),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = RESP_CANNOT_ARCHIVE),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public void archiveWorkItem(
            @Parameter(description = PARAM_WORK_ITEM_ID, required = true)
            @PathVariable UUID workItemId) {
        String userId = SecurityUtils.getCurrentUsername();
        workItemStateMachineService.archive(workItemId, userId);
    }

    @GetMapping("/by-workflow/{workflowDefinitionId}")
    @Operation(
            summary = SUMMARY_GET_BY_WORKFLOW,
            description = DESC_GET_BY_WORKFLOW
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_RETRIEVED_WORK_ITEMS,
                    content = @Content(schema = @Schema(implementation = WorkItemResponse.class))),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = INVALID_REQUEST)
    })
    public List<WorkItemResponse> getWorkItemsByWorkflowDefinition(
            @Parameter(description = PARAM_WORKFLOW_DEFINITION_ID, required = true)
            @PathVariable UUID workflowDefinitionId) {
        return workItemService.getWorkItemsByWorkflowDefinitionId(workflowDefinitionId);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkItemCreateResponse {
        private UUID workItemId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkItemSubmitResponse {
        private UUID versionId;
    }
}
