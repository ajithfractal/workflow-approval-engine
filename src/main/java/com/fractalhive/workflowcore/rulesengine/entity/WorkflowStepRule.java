package com.fractalhive.workflowcore.rulesengine.entity;

import com.fractalhive.workflowcore.common.entity.BaseEntity;
import com.fractalhive.workflowcore.rulesengine.enums.RuleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Represents a rule definition for a workflow step.
 * Rules can be used for auto-approval, skipping steps, routing approvers, etc.
 */
@Entity
@Table(name = "workflow_step_rule")
@Getter
@Setter
public class WorkflowStepRule extends BaseEntity {

    @Column(name = "step_definition_id", nullable = false, updatable = false)
    private UUID stepDefinitionId;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 50)
    private RuleType ruleType;

    /**
     * Rule expression in JSON format with SpEL (Spring Expression Language) conditions.
     * Format: {"condition": "variables['amount'] < 10000", "actions": {"autoApprove": true}}
     * Conditions use SpEL syntax and can access variables, context, task, step, etc.
     */
    @Column(name = "rule_expression", columnDefinition = "TEXT", nullable = false)
    private String ruleExpression;

    /**
     * Priority of the rule (higher number = higher priority).
     * Rules with higher priority are evaluated first.
     */
    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    /**
     * Whether this rule is currently active.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Optional description of what this rule does.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
