package com.fractalhive.workflowcore.rulesengine.dto;

import com.fractalhive.workflowcore.rulesengine.enums.RuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating a workflow step rule.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleCreateRequest {

    @NotNull(message = "Step definition ID is required")
    private UUID stepDefinitionId;

    @NotBlank(message = "Rule name is required")
    private String ruleName;

    @NotNull(message = "Rule type is required")
    private RuleType ruleType;

    @NotBlank(message = "Rule expression is required")
    private String ruleExpression;

    @Builder.Default
    private Integer priority = 0;

    @Builder.Default
    private Boolean isActive = true;

    private String description;

    /**
     * Optional conditions in JSON format (for simple rule engine).
     */
    private Map<String, Object> conditions;
}
