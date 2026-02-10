package com.fractalhive.workflowcore.approval.enums;

/**
 * Defines the type of approver assignment for a workflow step.
 */
public enum ApproverType {
    /**
     * Approver is a specific user identified by user ID.
     */
    USER,

    /**
     * Approver is a role, resolved to users at runtime.
     */
    ROLE,

    /**
     * Approver is resolved via manager chain hierarchy.
     */
    MANAGER
}
