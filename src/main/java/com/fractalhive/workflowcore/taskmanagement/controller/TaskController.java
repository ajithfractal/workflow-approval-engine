package com.fractalhive.workflowcore.taskmanagement.controller;

import com.fractalhive.workflowcore.approval.enums.DecisionType;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import com.fractalhive.workflowcore.approval.service.ApprovalTaskStateMachineService;
import com.fractalhive.workflowcore.taskmanagement.dto.*;
import com.fractalhive.workflowcore.taskmanagement.service.TaskManagementService;
import com.fractalhive.workflowcore.workflow.service.WorkflowOrchestratorService;
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
    public ResponseEntity<List<TaskResponse>> listTasks(
            @RequestParam String approverId,
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
    public ResponseEntity<TaskResponse> getTask(@PathVariable UUID taskId) {
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
    public ResponseEntity<Void> approveTask(
            @PathVariable UUID taskId,
            @RequestParam String userId,
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
    public ResponseEntity<Void> rejectTask(
            @PathVariable UUID taskId,
            @RequestParam String userId,
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
    public ResponseEntity<ApprovalCommentResponse> addComment(
            @PathVariable UUID taskId,
            @RequestParam String userId,
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
    public ResponseEntity<Void> delegateTask(
            @PathVariable UUID taskId,
            @RequestParam String userId,
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
    public ResponseEntity<TaskResponse> reassignTask(
            @PathVariable UUID taskId,
            @RequestParam String userId,
            @Valid @RequestBody TaskReassignRequest request) {
        TaskResponse task = taskManagementService.reassignTask(taskId, request, userId);
        return ResponseEntity.ok(task);
    }

}
