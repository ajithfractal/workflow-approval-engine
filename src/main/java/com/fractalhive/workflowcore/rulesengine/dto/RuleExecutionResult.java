package com.fractalhive.workflowcore.rulesengine.dto;

import com.fractalhive.workflowcore.rulesengine.enums.SlaAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Result of rule execution, containing actions to take.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleExecutionResult {

    /**
     * Whether remaining tasks should be auto-approved.
     */
    @Builder.Default
    private Boolean shouldAutoApprove = false;

    /**
     * Whether the current step should be skipped.
     */
    @Builder.Default
    private Boolean shouldSkipStep = false;

    /**
     * New approver ID for routing (if rule changes approver).
     */
    private String newApproverId;

    /**
     * SLA action to take (if SLA_ACTION rule type).
     */
    private SlaAction slaAction;

    /**
     * Additional metadata for custom actions.
     */
    private Map<String, Object> metadata;

    /**
     * Whether any rule matched and produced a result.
     */
    @Builder.Default
    private Boolean ruleMatched = false;
}
