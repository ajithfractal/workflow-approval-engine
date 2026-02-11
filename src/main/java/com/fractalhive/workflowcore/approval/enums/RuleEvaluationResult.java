package com.fractalhive.workflowcore.approval.enums;

/**
 * Result of evaluating approval rules for a workflow step.
 */
public enum RuleEvaluationResult {
    /**
     * Step approval criteria has been met - step can complete.
     */
    COMPLETE,

    /**
     * Step has been rejected - workflow should be rejected or trigger rework.
     */
    REJECTED,

    /**
     * Step is still pending - waiting for more approvals.
     */
    PENDING
}
