package com.fractalhive.workflowcore.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for workflow definition with steps and approvers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinitionResponse {

    private UUID workflowId;
    private String name;
    private Integer version;
    private Boolean isActive;
    private List<StepDefinitionResponse> steps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepDefinitionResponse {
        private UUID stepId;
        private String stepName;
        private Integer stepOrder;
        private String approvalType;
        private Integer minApprovals;
        private Integer slaHours;
        private List<ApproverResponse> approvers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApproverResponse {
        private UUID approverId;
        private String approverType;
        private String approverValue;
    }
}
