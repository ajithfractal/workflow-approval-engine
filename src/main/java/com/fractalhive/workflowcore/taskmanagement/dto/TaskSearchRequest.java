package com.fractalhive.workflowcore.taskmanagement.dto;

import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for searching approval tasks with filters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSearchRequest {

    /**
     * Approver ID - required filter.
     */
    @NotBlank(message = "approverId is required")
    private String approverId;

    /**
     * Optional status filter.
     */
    private TaskStatus status;

    /**
     * Optional free-text search (matches approverId or approverType).
     */
    private String search;

    /**
     * Optional epoch millis range start.
     */
    private Long startTime;

    /**
     * Optional epoch millis range end.
     */
    private Long endTime;

    /**
     * Optional field selector for time range filter.
     * Valid values: "dueAt" or "createdAt" (default: "createdAt").
     */
    private String timeCheckIn;
}
