package com.fractalhive.workflowcore.rulesengine.dto;

import com.fractalhive.workflowcore.rulesengine.entity.WorkflowStepRule;
import com.fractalhive.workflowcore.rulesengine.enums.RuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Response DTO for workflow step rules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResponse {

    private UUID id;
    private UUID stepDefinitionId;
    private String ruleName;
    private RuleType ruleType;
    private String ruleExpression;
    private Integer priority;
    private Boolean isActive;
    private String description;
    private Timestamp createdAt;
    private String createdBy;
    private Timestamp updatedAt;
    private String updatedBy;

    public static RuleResponse fromEntity(WorkflowStepRule rule) {
        return RuleResponse.builder()
                .id(rule.getId())
                .stepDefinitionId(rule.getStepDefinitionId())
                .ruleName(rule.getRuleName())
                .ruleType(rule.getRuleType())
                .ruleExpression(rule.getRuleExpression())
                .priority(rule.getPriority())
                .isActive(rule.getIsActive())
                .description(rule.getDescription())
                .createdAt(rule.getCreatedAt())
                .createdBy(rule.getCreatedBy())
                .updatedAt(rule.getUpdatedAt())
                .updatedBy(rule.getUpdatedBy())
                .build();
    }
}
