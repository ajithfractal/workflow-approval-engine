package com.fractalhive.workflowcore.taskmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for reassigning a task to a new approver.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskReassignRequest {

    /**
     * The new approver ID to assign the task to.
     */
    private String newApproverId;

    /**
     * Optional reason for reassignment.
     * This will be saved as a comment on the task.
     */
    private String reason;
}
