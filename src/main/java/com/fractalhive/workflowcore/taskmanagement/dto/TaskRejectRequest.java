package com.fractalhive.workflowcore.taskmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for rejecting a task.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRejectRequest {

    /**
     * Optional comments explaining the rejection.
     */
    private String comments;
}
