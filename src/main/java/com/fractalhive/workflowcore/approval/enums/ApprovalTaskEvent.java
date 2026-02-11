package com.fractalhive.workflowcore.approval.enums;

/**
 * Events that drive the Approval Task State Machine.
 */
public enum ApprovalTaskEvent {
    /**
     * Approver approves the task.
     */
    APPROVE,

    /**
     * Approver rejects the task.
     */
    REJECT,

    /**
     * Approver delegates the task to another user.
     */
    DELEGATE,

    /**
     * SLA breach detected - task expired.
     */
    SLA_BREACH,

    /**
     * Parent workflow was cancelled.
     */
    WORKFLOW_CANCELLED,

    /**
     * Delegate accepts the delegated task.
     */
    ACCEPT
}
