package com.fractalhive.workflowcore.taskmanagement.controller;

import com.fractalhive.workflowcore.approval.enums.DecisionType;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import com.fractalhive.workflowcore.approval.service.ApprovalTaskStateMachineService;
import com.fractalhive.workflowcore.taskmanagement.dto.*;
import com.fractalhive.workflowcore.taskmanagement.service.TaskManagementService;
import com.fractalhive.workflowcore.workflow.service.WorkflowOrchestratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for approval task operations.
 * Provides endpoints for approvers to manage their tasks.
 */
@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "APIs for managing approval tasks. Approvers can list, view, approve, reject, comment, delegate, and reassign tasks.")
public class TaskController {

    private final TaskManagementService taskManagementService;
    private final WorkflowOrchestratorService orchestratorService;
    private final ApprovalTaskStateMachineService approvalTaskStateMachineService;

    public TaskController(TaskManagementService taskManagementService,
                         WorkflowOrchestratorService orchestratorService,
                         ApprovalTaskStateMachineService approvalTaskStateMachineService) {
        this.taskManagementService = taskManagementService;
        this.orchestratorService = orchestratorService;
        this.approvalTaskStateMachineService = approvalTaskStateMachineService;
    }

    /**
     * Lists tasks for a specific approver.
     *
     * @param approverId the approver user ID
     * @param status     optional task status filter (null for all statuses)
     * @return list of tasks
     */
    @GetMapping
    @Operation(
            summary = "List tasks for an approver",
            description = "Retrieves all tasks assigned to a specific approver, optionally filtered by status"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved tasks",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<List<TaskResponse>> listTasks(
            @Parameter(description = "The approver user ID", required = true, example = "user123")
            @RequestParam String approverId,
            @Parameter(description = "Optional task status filter", example = "PENDING")
            @RequestParam(required = false) TaskStatus status) {
        List<TaskResponse> tasks = taskManagementService.getTasksByApprover(approverId, status);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Gets task details by task ID.
     *
     * @param taskId the task ID
     * @return task details
     */
    @GetMapping("/{taskId}")
    @Operation(
            summary = "Get task by ID",
            description = "Retrieves detailed information about a specific approval task"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task found",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskResponse> getTask(
            @Parameter(description = "The task ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID taskId) {
        TaskResponse task = taskManagementService.getTask(taskId);
        return ResponseEntity.ok(task);
    }

    /**
     * Approves a task.
     * This will record the approval decision and automatically advance the workflow if step completion criteria is met.
     *
     * @param taskId  the task ID
     * @param userId  the user ID approving the task
     * @param request the approval request with optional comments
     * @return no content
     */
    @PostMapping("/{taskId}/approve")
    @Operation(
            summary = "Approve a task",
            description = "Records an approval decision for a task and advances the workflow if step completion criteria is met"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task approved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or task cannot be approved"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Void> approveTask(
            @Parameter(description = "The task ID", required = true)
            @PathVariable UUID taskId,
            @Parameter(description = "The user ID approving the task", required = true, example = "user123")
            @RequestParam String userId,
            @Parameter(description = "Approval request with optional comments")
            @Valid @RequestBody(required = false) TaskApproveRequest request) {
        String comments = request != null ? request.getComments() : null;
        orchestratorService.handleApprovalDecision(taskId, userId, DecisionType.APPROVED, comments);
        return ResponseEntity.ok().build();
    }

    /**
     * Rejects a task.
     * This will record the rejection decision and fail the workflow if step completion criteria requires all approvals.
     *
     * @param taskId  the task ID
     * @param userId  the user ID rejecting the task
     * @param request the rejection request with optional comments
     * @return no content
     */
    @PostMapping("/{taskId}/reject")
    @Operation(
            summary = "Reject a task",
            description = "Records a rejection decision for a task and fails the workflow if step completion criteria requires all approvals"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task rejected successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or task cannot be rejected"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Void> rejectTask(
            @Parameter(description = "The task ID", required = true)
            @PathVariable UUID taskId,
            @Parameter(description = "The user ID rejecting the task", required = true, example = "user123")
            @RequestParam String userId,
            @Parameter(description = "Rejection request with optional comments")
            @Valid @RequestBody(required = false) TaskRejectRequest request) {
        String comments = request != null ? request.getComments() : null;
        orchestratorService.handleApprovalDecision(taskId, userId, DecisionType.REJECTED, comments);
        return ResponseEntity.ok().build();
    }

    /**
     * Adds a comment to a task.
     *
     * @param taskId  the task ID
     * @param userId  the user ID adding the comment
     * @param request the comment request
     * @return the created comment
     */
    @PostMapping("/{taskId}/comments")
    @Operation(
            summary = "Add comment to a task",
            description = "Adds a comment to an approval task"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Comment created successfully",
                    content = @Content(schema = @Schema(implementation = ApprovalCommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<ApprovalCommentResponse> addComment(
            @Parameter(description = "The task ID", required = true)
            @PathVariable UUID taskId,
            @Parameter(description = "The user ID adding the comment", required = true, example = "user123")
            @RequestParam String userId,
            @Parameter(description = "Comment request")
            @Valid @RequestBody TaskCommentRequest request) {
        ApprovalCommentResponse comment = taskManagementService.addComment(taskId, request.getComment(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    /**
     * Delegates a task to another approver.
     * Delegation is a task-level operation and does not advance the workflow.
     *
     * @param taskId  the task ID
     * @param userId  the current approver user ID
     * @param request the delegation request with target user ID and optional reason
     * @return no content
     */
    @PostMapping("/{taskId}/delegate")
    @Operation(
            summary = "Delegate a task",
            description = "Delegates a task to another approver. This is a task-level operation and does not advance the workflow"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task delegated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or task cannot be delegated"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Void> delegateTask(
            @Parameter(description = "The task ID", required = true)
            @PathVariable UUID taskId,
            @Parameter(description = "The current approver user ID", required = true, example = "user123")
            @RequestParam String userId,
            @Parameter(description = "Delegation request with target user ID and optional reason")
            @Valid @RequestBody TaskDelegateRequest request) {
        approvalTaskStateMachineService.delegate(taskId, userId, request.getToUserId());
        return ResponseEntity.ok().build();
    }

    /**
     * Reassigns a task to a new approver.
     *
     * @param taskId  the task ID
     * @param userId  the user ID performing the reassignment
     * @param request the reassignment request
     * @return updated task details
     */
    @PostMapping("/{taskId}/reassign")
    @Operation(
            summary = "Reassign a task",
            description = "Reassigns a task to a new approver"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task reassigned successfully",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskResponse> reassignTask(
            @Parameter(description = "The task ID", required = true)
            @PathVariable UUID taskId,
            @Parameter(description = "The user ID performing the reassignment", required = true, example = "user123")
            @RequestParam String userId,
            @Parameter(description = "Reassignment request")
            @Valid @RequestBody TaskReassignRequest request) {
        TaskResponse task = taskManagementService.reassignTask(taskId, request, userId);
        return ResponseEntity.ok(task);
    }

}
