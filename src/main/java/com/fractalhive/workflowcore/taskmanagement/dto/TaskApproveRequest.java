package com.fractalhive.workflowcore.taskmanagement.dto;

import com.fractalhive.workflowcore.approval.enums.DecisionType;

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
    private DecisionType decisionType;
}
