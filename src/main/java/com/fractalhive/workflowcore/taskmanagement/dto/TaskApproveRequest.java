package com.fractalhive.workflowcore.taskmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for approving a task.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskApproveRequest {

    /**
     * Optional comments for the approval.
     */
    private String comments;
}
