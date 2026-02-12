package com.fractalhive.workflowcore.workitem.controller;

import com.fractalhive.workflowcore.workitem.dto.WorkItemCreateRequest;
import com.fractalhive.workflowcore.workitem.dto.WorkItemResponse;
import com.fractalhive.workflowcore.workitem.dto.WorkItemSubmitRequest;
import com.fractalhive.workflowcore.workitem.dto.WorkItemVersionResponse;
import com.fractalhive.workflowcore.workitem.dto.WorkflowProgressResponse;
import com.fractalhive.workflowcore.workitem.service.WorkItemService;
import com.fractalhive.workflowcore.workitem.statemachine.service.WorkItemStateMachineService;
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
public class WorkItemController {

    private final WorkItemService workItemService;
    private final WorkItemStateMachineService workItemStateMachineService;

    public WorkItemController(WorkItemService workItemService,
                              WorkItemStateMachineService workItemStateMachineService) {
        this.workItemService = workItemService;
        this.workItemStateMachineService = workItemStateMachineService;
    }

    /**
     * Creates a new work item.
     *
     * @param request   the work item creation request
     * @param createdBy the user creating the work item
     * @return the created work item ID
     */
    @PostMapping
    public ResponseEntity<WorkItemCreateResponse> createWorkItem(
            @Valid @RequestBody WorkItemCreateRequest request,
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
    public ResponseEntity<WorkItemResponse> getWorkItem(@PathVariable UUID workItemId) {
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
    public ResponseEntity<WorkItemSubmitResponse> submitWorkItem(
            @PathVariable UUID workItemId,
            @Valid @RequestBody WorkItemSubmitRequest request,
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
    public ResponseEntity<List<WorkItemVersionResponse>> getVersions(@PathVariable UUID workItemId) {
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
    public ResponseEntity<WorkflowProgressResponse> getWorkflowProgress(@PathVariable UUID workItemId) {
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
    public ResponseEntity<Void> archiveWorkItem(
            @PathVariable UUID workItemId,
            @RequestParam String userId) {
        workItemStateMachineService.archive(workItemId, userId);
        return ResponseEntity.ok().build();
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
