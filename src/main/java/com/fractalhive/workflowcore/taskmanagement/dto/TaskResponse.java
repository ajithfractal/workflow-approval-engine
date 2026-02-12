package com.fractalhive.workflowcore.taskmanagement.dto;

import com.fractalhive.workflowcore.approval.enums.ApproverType;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Full task details response DTO.
 * Contains complete task information including comments and decisions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private UUID taskId;
    private UUID stepInstanceId;
    private String approverId;
    private ApproverType approverType;
    private TaskStatus status;
    private Timestamp dueAt;
    private Timestamp actedAt;
    private Timestamp createdAt;
    private String createdBy;

    /**
     * List of comments on this task.
     */
    private List<ApprovalCommentResponse> comments;

    /**
     * List of decisions made on this task.
     */
    private List<ApprovalDecisionResponse> decisions;

    /**
     * Step instance details.
     */
    private StepInstanceInfo stepInstance;

    /**
     * Workflow instance details.
     */
    private WorkflowInstanceInfo workflowInstance;

    /**
     * Work item details.
     */
    private WorkItemInfo workItem;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepInstanceInfo {
        private UUID stepInstanceId;
        private UUID stepId;
        private String stepName;
        private Integer stepOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowInstanceInfo {
        private UUID workflowInstanceId;
        private UUID workflowId;
        private String workflowName;
        private String workflowStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkItemInfo {
        private UUID workItemId;
        private String type;
        private Integer currentVersion;
        private String workItemStatus;
    }
}
