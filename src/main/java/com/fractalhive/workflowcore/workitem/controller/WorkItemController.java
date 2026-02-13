package com.fractalhive.workflowcore.workitem.controller;

import com.fractalhive.workflowcore.workitem.dto.WorkItemCreateRequest;
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
import org.springframework.http.ResponseEntity;
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

    /**
     * Lists all work items, optionally filtered by status and/or type.
     *
     * @param status optional status filter
     * @param type   optional type filter
     * @return list of work items
     */
    @GetMapping
    @Operation(
            summary = "List work items",
            description = "Retrieves all work items, optionally filtered by status and/or type. Results are ordered by creation date (newest first)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved work items",
                    content = @Content(schema = @Schema(implementation = WorkItemResponse.class)))
    })
    public ResponseEntity<List<WorkItemResponse>> listWorkItems(
            @Parameter(description = "Optional status filter", example = "DRAFT")
            @RequestParam(required = false) WorkItemStatus status,
            @Parameter(description = "Optional type filter", example = "contract")
            @RequestParam(required = false) String type) {
        List<WorkItemResponse> workItems = workItemService.listWorkItems(status, type);
        return ResponseEntity.ok(workItems);
    }

    /**
     * Creates a new work item.
     *
     * @param request   the work item creation request
     * @param createdBy the user creating the work item
     * @return the created work item ID
     */
    @PostMapping
    @Operation(
            summary = "Create a new work item",
            description = "Creates a new work item that can be submitted for approval workflow"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Work item created successfully",
                    content = @Content(schema = @Schema(implementation = WorkItemCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<WorkItemCreateResponse> createWorkItem(
            @Parameter(description = "Work item creation request")
            @Valid @RequestBody WorkItemCreateRequest request,
            @Parameter(description = "User ID creating the work item", required = true, example = "user123")
            @RequestParam String createdBy) {
        UUID workItemId = workItemService.createWorkItem(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(WorkItemCreateResponse.builder().workItemId(workItemId).build());
    }

    /**
     * Gets a work item by ID.
     *
     * @param workItemId the work item ID
     * @return the work item details
     */
    @GetMapping("/{workItemId}")
    @Operation(
            summary = "Get work item by ID",
            description = "Retrieves detailed information about a specific work item"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Work item found",
                    content = @Content(schema = @Schema(implementation = WorkItemResponse.class))),
            @ApiResponse(responseCode = "404", description = "Work item not found")
    })
    public ResponseEntity<WorkItemResponse> getWorkItem(
            @Parameter(description = "The work item ID", required = true)
            @PathVariable UUID workItemId) {
        WorkItemResponse workItem = workItemService.getWorkItem(workItemId);
        return ResponseEntity.ok(workItem);
    }

    /**
     * Submits a work item for approval.
     *
     * @param workItemId  the work item ID
     * @param request     the submission request
     * @param submittedBy the user submitting the work item
     * @return the created version ID
     */
    @PostMapping("/{workItemId}/submit")
    @Operation(
            summary = "Submit work item for approval",
            description = "Submits a work item to start the approval workflow process"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Work item submitted successfully",
                    content = @Content(schema = @Schema(implementation = WorkItemSubmitResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or work item cannot be submitted"),
            @ApiResponse(responseCode = "404", description = "Work item not found")
    })
    public ResponseEntity<WorkItemSubmitResponse> submitWorkItem(
            @Parameter(description = "The work item ID", required = true)
            @PathVariable UUID workItemId,
            @Parameter(description = "Submission request with workflow ID")
            @Valid @RequestBody WorkItemSubmitRequest request,
            @Parameter(description = "User ID submitting the work item", required = true, example = "user123")
            @RequestParam String submittedBy) {
        UUID versionId = workItemService.submitWorkItem(workItemId, request, submittedBy);
        return ResponseEntity.ok(WorkItemSubmitResponse.builder().versionId(versionId).build());
    }

    /**
     * Gets all versions for a work item.
     *
     * @param workItemId the work item ID
     * @return list of work item versions
     */
    @GetMapping("/{workItemId}/versions")
    @Operation(
            summary = "Get work item versions",
            description = "Retrieves all versions of a work item"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved versions",
                    content = @Content(schema = @Schema(implementation = WorkItemVersionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Work item not found")
    })
    public ResponseEntity<List<WorkItemVersionResponse>> getVersions(
            @Parameter(description = "The work item ID", required = true)
            @PathVariable UUID workItemId) {
        List<WorkItemVersionResponse> versions = workItemService.getVersions(workItemId);
        return ResponseEntity.ok(versions);
    }

    /**
     * Gets workflow progress for a work item.
     * Shows which step is currently in progress, completed steps, and overall progress.
     *
     * @param workItemId the work item ID
     * @return workflow progress information
     */
    @GetMapping("/{workItemId}/workflow-progress")
    @Operation(
            summary = "Get workflow progress",
            description = "Retrieves detailed workflow progress information including current step, completed steps, and overall progress"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved progress",
                    content = @Content(schema = @Schema(implementation = WorkflowProgressResponse.class))),
            @ApiResponse(responseCode = "404", description = "Work item not found")
    })
    public ResponseEntity<WorkflowProgressResponse> getWorkflowProgress(
            @Parameter(description = "The work item ID", required = true)
            @PathVariable UUID workItemId) {
        WorkflowProgressResponse progress = workItemService.getWorkflowProgress(workItemId);
        return ResponseEntity.ok(progress);
    }

    /**
     * Archives a work item.
     * Transitions the work item from APPROVED or REJECTED status to ARCHIVED.
     *
     * @param workItemId the work item ID
     * @param userId     the user archiving the work item
     * @return no content
     */
    @PostMapping("/{workItemId}/archive")
    @Operation(
            summary = "Archive work item",
            description = "Manually archives a work item, transitioning it from APPROVED or REJECTED status to ARCHIVED"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Work item archived successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or work item cannot be archived"),
            @ApiResponse(responseCode = "404", description = "Work item not found")
    })
    public ResponseEntity<Void> archiveWorkItem(
            @Parameter(description = "The work item ID", required = true)
            @PathVariable UUID workItemId,
            @Parameter(description = "User ID archiving the work item", required = true, example = "user123")
            @RequestParam String userId) {
        workItemStateMachineService.archive(workItemId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Gets all work items associated with a workflow definition.
     *
     * @param workflowDefinitionId the workflow definition ID
     * @return list of work items
     */
    @GetMapping("/by-workflow/{workflowDefinitionId}")
    @Operation(
            summary = "Get work items by workflow definition ID",
            description = "Retrieves all work items that have workflow instances using the specified workflow definition"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved work items",
                    content = @Content(schema = @Schema(implementation = WorkItemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid workflow definition ID")
    })
    public ResponseEntity<List<WorkItemResponse>> getWorkItemsByWorkflowDefinition(
            @Parameter(description = "The workflow definition ID", required = true)
            @PathVariable UUID workflowDefinitionId) {
        List<WorkItemResponse> workItems = workItemService.getWorkItemsByWorkflowDefinitionId(workflowDefinitionId);
        return ResponseEntity.ok(workItems);
    }

    /**
     * Response DTO for work item creation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkItemCreateResponse {
        private UUID workItemId;
    }

    /**
     * Response DTO for work item submission.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkItemSubmitResponse {
        private UUID versionId;
    }
}
