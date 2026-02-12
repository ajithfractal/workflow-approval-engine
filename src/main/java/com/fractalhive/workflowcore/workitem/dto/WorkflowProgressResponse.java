package com.fractalhive.workflowcore.workitem.dto;

import com.fractalhive.workflowcore.workflow.enums.StepStatus;
import com.fractalhive.workflowcore.workflow.enums.WorkflowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for workflow progress information.
 * Shows current step, all steps with their statuses, and progress summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowProgressResponse {
    private UUID workItemId;
    private WorkflowInstanceInfo workflowInstance;
    private List<StepProgressInfo> steps;
    private StepProgressInfo currentStep;
    private ProgressSummary progress;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowInstanceInfo {
        private UUID workflowInstanceId;
        private UUID workflowId;
        private String workflowName;
        private Integer workflowVersion;
        private WorkflowStatus status;
        private Timestamp startedAt;
        private Timestamp completedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepProgressInfo {
        private UUID stepInstanceId;
        private UUID stepId;
        private String stepName;
        private Integer stepOrder;
        private StepStatus status;
        private Timestamp startedAt;
        private Timestamp completedAt;
        private List<TaskProgressInfo> tasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskProgressInfo {
        private UUID taskId;
        private String approverId;
        private com.fractalhive.workflowcore.approval.enums.TaskStatus status;
        private Timestamp dueAt;
        private Timestamp actedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressSummary {
        private int completedSteps;
        private int totalSteps;
        private int percentage;
    }
}
