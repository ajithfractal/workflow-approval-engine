package com.fractalhive.workflowcore.rulesengine.enums;

/**
 * Actions to take when SLA is breached or approaching breach.
 */
public enum SlaAction {
    /**
     * Delegate the task to a senior approver.
     */
    DELEGATE_TO_SENIOR,

    /**
     * Escalate the task to a higher authority.
     */
    ESCALATE,

    /**
     * Expire the task due to SLA breach.
     */
    EXPIRE
}
