package com.fractalhive.workflowcore.taskmanagement.service;

import com.fractalhive.workflowcore.approval.dto.ApprovalTaskCreateRequest;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import com.fractalhive.workflowcore.taskmanagement.dto.TaskReassignRequest;
import com.fractalhive.workflowcore.taskmanagement.dto.TaskResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing approval tasks.
 * Handles task creation and basic fetching operations.
 */
public interface TaskManagementService {

    /**
     * Creates a single approval task from a DTO.
     *
     * @param request   the task creation request
     * @param createdBy the user ID creating the task
     * @return the created task ID
     */
    UUID createTask(ApprovalTaskCreateRequest request, String createdBy);

    /**
     * Creates tasks for all approvers defined in a workflow step.
     * Resolves approvers from WorkflowStepApprover and creates one task per approver.
     *
     * @param stepInstanceId the step instance ID
     * @param createdBy      the user ID creating the tasks
     * @return list of created task IDs
     */
    List<UUID> createTasksForStep(UUID stepInstanceId, String createdBy);

    /**
     * Gets full task details including comments and decisions.
     *
     * @param taskId the task ID
     * @return full task details
     */
    TaskResponse getTask(UUID taskId);

    /**
     * Gets tasks for a specific approver filtered by status.
     *
     * @param approverId the approver ID
     * @param status     the task status (null for all statuses)
     * @return list of task responses
     */
    List<TaskResponse> getTasksByApprover(String approverId, TaskStatus status);

    /**
     * Reassigns a task to a new approver.
     *
     * @param taskId the task ID
     * @param request the reassignment request
     * @param userId the user ID performing the reassignment
     * @return updated task details
     */
    TaskResponse reassignTask(UUID taskId, TaskReassignRequest request, String userId);
}
