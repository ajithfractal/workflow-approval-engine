package com.fractalhive.workflowcore.taskmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for delegating a task to another approver.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDelegateRequest {

    /**
     * The user ID to delegate the task to.
     */
    @NotBlank(message = "To user ID is required")
    private String toUserId;

    /**
     * Optional reason for delegation.
     */
    private String reason;
}
