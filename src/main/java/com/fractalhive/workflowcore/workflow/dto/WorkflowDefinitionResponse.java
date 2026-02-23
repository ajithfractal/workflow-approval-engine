package com.fractalhive.workflowcore.workflow.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for workflow definition with stages, steps and approvers.
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
    private List<StageDefinitionResponse> stages;
    private Map<String, Object> visualStructure;
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageDefinitionResponse {
        private UUID stageId;
        private String stageName;
        private Integer stageOrder;
        private String stepCompletionType;
        private Integer minStepCompletions;
        private List<StepDefinitionResponse> steps;
    }

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
