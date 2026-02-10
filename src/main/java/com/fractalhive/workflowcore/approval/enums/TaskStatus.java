package com.fractalhive.workflowcore.approval.enums;

/**
 * Status of an approval task.
 */
public enum TaskStatus {
    /**
     * Task is pending approval.
     */
    PENDING,

    /**
     * Task has been approved.
     */
    APPROVED,

    /**
     * Task has been rejected.
     */
    REJECTED,

    /**
     * Task has been delegated to another approver.
     */
    DELEGATED,

    /**
     * Task SLA has been breached and expired.
     */
    EXPIRED,

    /**
     * Task has been cancelled.
     */
    CANCELLED
}
