package com.fractalhive.workflowcore.taskmanagement.controller;

import static com.fractalhive.workflowcore.common.constants.ApiConstants.Common.INVALID_REQUEST;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Common.NOT_FOUND;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ResponseCodes.BAD_REQUEST_400;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ResponseCodes.CREATED_201;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ResponseCodes.INVALID_REQUEST_PARAMETERS;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ResponseCodes.NOT_FOUND_404;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ResponseCodes.OK_200;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.DESC_ADD_COMMENT;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.DESC_APPROVE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.DESC_APPROVE_OR_REJECT;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.DESC_DELEGATE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.DESC_LIST;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.DESC_REASSIGN;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.DESC_REJECT;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.DESC_SEARCH;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.PARAM_APPROVER_ID;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.PARAM_APPROVE_REQUEST;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.PARAM_COMMENT_REQUEST;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.PARAM_DELEGATE_REQUEST;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.PARAM_PAGE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.PARAM_REASSIGN_REQUEST;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.PARAM_REJECT_REQUEST;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.PARAM_SEARCH_REQUEST;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.PARAM_SIZE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.PARAM_SORT_BY;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.PARAM_SORT_DIRECTION;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.PARAM_STATUS_FILTER;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.PARAM_TASK_ID;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.RESP_APPROVED_SUCCESS;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.RESP_CANNOT_APPROVE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.RESP_CANNOT_DELEGATE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.RESP_CANNOT_REJECT;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.RESP_COMMENT_CREATED;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.RESP_DELEGATED_SUCCESS;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.RESP_FOUND;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.RESP_REASSIGNED_SUCCESS;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.RESP_REJECTED_SUCCESS;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.RESP_RETRIEVED_TASKS;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.SUMMARY_ADD_COMMENT;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.SUMMARY_APPROVE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.SUMMARY_APPROVE_OR_REJECT;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.SUMMARY_DELEGATE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.SUMMARY_LIST;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.SUMMARY_REASSIGN;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.SUMMARY_REJECT;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Task.SUMMARY_SEARCH;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowDefinition.DESC_GET_BY_ID;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowDefinition.SUMMARY_GET_BY_ID;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fractalhive.keycloak.util.SecurityUtils;
import com.fractalhive.workflowcore.approval.enums.DecisionType;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import com.fractalhive.workflowcore.approval.service.ApprovalTaskStateMachineService;
import com.fractalhive.workflowcore.common.dto.PaginatedResponse;
import com.fractalhive.workflowcore.taskmanagement.dto.ApprovalCommentResponse;
import com.fractalhive.workflowcore.taskmanagement.dto.TaskApproveRequest;
import com.fractalhive.workflowcore.taskmanagement.dto.TaskCommentRequest;
import com.fractalhive.workflowcore.taskmanagement.dto.TaskDelegateRequest;
import com.fractalhive.workflowcore.taskmanagement.dto.TaskReassignRequest;
import com.fractalhive.workflowcore.taskmanagement.dto.TaskRejectRequest;
import com.fractalhive.workflowcore.taskmanagement.dto.TaskResponse;
import com.fractalhive.workflowcore.taskmanagement.dto.TaskSearchRequest;
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

    @GetMapping
    @Operation(
            summary = SUMMARY_LIST,
            description = DESC_LIST
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_RETRIEVED_TASKS,
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = INVALID_REQUEST_PARAMETERS)
    })
    public List<TaskResponse> listTasks(
            @Parameter(description = PARAM_APPROVER_ID, required = true, example = "user123")
            @RequestParam String approverId,
            @Parameter(description = PARAM_STATUS_FILTER, example = "PENDING")
            @RequestParam(required = false) TaskStatus status) {
        return taskManagementService.getTasksByApprover(approverId, status);
    }

    @GetMapping("/{taskId}")
    @Operation(
            summary = SUMMARY_GET_BY_ID,
            description = DESC_GET_BY_ID
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_FOUND,
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public TaskResponse getTask(
            @Parameter(description = PARAM_TASK_ID, required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID taskId) {
        return taskManagementService.getTask(taskId);
    }
    
    @PostMapping("/{taskId}/actions")
    @Operation(
            summary = SUMMARY_APPROVE_OR_REJECT,
            description = DESC_APPROVE_OR_REJECT
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_APPROVED_SUCCESS),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = RESP_CANNOT_APPROVE),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public void approveOrRejectTask(
            @Parameter(description = PARAM_TASK_ID, required = true)
            @PathVariable UUID taskId,
            @Parameter(description = PARAM_APPROVE_REQUEST)
            @Valid @RequestBody(required = true) TaskApproveRequest request) {
		String userId = SecurityUtils.getCurrentUsername();
		orchestratorService.handleApprovalDecision(taskId, userId, request.getDecisionType(), request.getComments());
    }


    @PostMapping("/{taskId}/approve")
    @Operation(
            summary = SUMMARY_APPROVE,
            description = DESC_APPROVE
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_APPROVED_SUCCESS),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = RESP_CANNOT_APPROVE),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public void approveTask(
            @Parameter(description = PARAM_TASK_ID, required = true)
            @PathVariable UUID taskId,
            @Parameter(description = PARAM_APPROVE_REQUEST)
            @Valid @RequestBody(required = false) TaskApproveRequest request) {
        String userId = SecurityUtils.getCurrentUsername();
        String comments = request != null ? request.getComments() : null;
        orchestratorService.handleApprovalDecision(taskId, userId, DecisionType.APPROVED, comments);
    }

    @PostMapping("/{taskId}/reject")
    @Operation(
            summary = SUMMARY_REJECT,
            description = DESC_REJECT
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_REJECTED_SUCCESS),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = RESP_CANNOT_REJECT),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public void rejectTask(
            @Parameter(description = PARAM_TASK_ID, required = true)
            @PathVariable UUID taskId,
            @Parameter(description = PARAM_REJECT_REQUEST)
            @Valid @RequestBody(required = false) TaskRejectRequest request) {
        String userId = SecurityUtils.getCurrentUsername();
        String comments = request != null ? request.getComments() : null;
        orchestratorService.handleApprovalDecision(taskId, userId, DecisionType.REJECTED, comments);
    }

    @PostMapping("/{taskId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = SUMMARY_ADD_COMMENT,
            description = DESC_ADD_COMMENT
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = CREATED_201, description = RESP_COMMENT_CREATED,
                    content = @Content(schema = @Schema(implementation = ApprovalCommentResponse.class))),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = INVALID_REQUEST),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public ApprovalCommentResponse addComment(
            @Parameter(description = PARAM_TASK_ID, required = true)
            @PathVariable UUID taskId,
            @Parameter(description = PARAM_COMMENT_REQUEST)
            @Valid @RequestBody TaskCommentRequest request) {
        String userId = SecurityUtils.getCurrentUsername();
        return taskManagementService.addComment(taskId, request.getComment(), userId);
    }

    @PostMapping("/{taskId}/delegate")
    @Operation(
            summary = SUMMARY_DELEGATE,
            description = DESC_DELEGATE
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_DELEGATED_SUCCESS),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = RESP_CANNOT_DELEGATE),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public void delegateTask(
            @Parameter(description = PARAM_TASK_ID, required = true)
            @PathVariable UUID taskId,
            @Parameter(description = PARAM_DELEGATE_REQUEST)
            @Valid @RequestBody TaskDelegateRequest request) {
        String userId = SecurityUtils.getCurrentUsername();
        approvalTaskStateMachineService.delegate(taskId, userId, request.getToUserId());
    }

    @PostMapping("/{taskId}/reassign")
    @Operation(
            summary = SUMMARY_REASSIGN,
            description = DESC_REASSIGN
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_REASSIGNED_SUCCESS,
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = INVALID_REQUEST),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public TaskResponse reassignTask(
            @Parameter(description = PARAM_TASK_ID, required = true)
            @PathVariable UUID taskId,
            @Parameter(description = PARAM_REASSIGN_REQUEST)
            @Valid @RequestBody TaskReassignRequest request) {
        String userId = SecurityUtils.getCurrentUsername();
        return taskManagementService.reassignTask(taskId, request, userId);
    }

    @PostMapping("/search")
    @Operation(
            summary = SUMMARY_SEARCH,
            description = DESC_SEARCH
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_RETRIEVED_TASKS,
                    content = @Content(schema = @Schema(implementation = PaginatedResponse.class))),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = INVALID_REQUEST_PARAMETERS)
    })
    public PaginatedResponse<TaskResponse> searchTasks(
            @Parameter(description = PARAM_SEARCH_REQUEST)
            @Valid @RequestBody TaskSearchRequest request,
            @Parameter(description = PARAM_PAGE, example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = PARAM_SIZE, example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = PARAM_SORT_BY, example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = PARAM_SORT_DIRECTION, example = "DESC")
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return taskManagementService.searchTasks(request, pageable);
    }

}
