package com.fractalhive.workflowcore.rulesengine.enums;

/**
 * Types of rules that can be defined for workflow steps.
 */
public enum RuleType {
    /**
     * Auto-approve tasks based on conditions (e.g., amount < threshold).
     */
    AUTO_APPROVE,

    /**
     * Skip a step entirely based on conditions (e.g., skip legal review for pre-approved templates).
     */
    SKIP_STEP,

    /**
     * Route to a different approver based on conditions (e.g., route to senior if risk score high).
     */
    ROUTE_APPROVER,

    /**
     * SLA-based actions (delegate to senior, escalate, expire).
     */
    SLA_ACTION,

    /**
     * Conditional routing based on dynamic conditions.
     */
    CONDITIONAL_ROUTING
}
