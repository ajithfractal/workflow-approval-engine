package com.fractalhive.workflowcore.workitem.statemachine.enums;

/**
 * Events for the Work Item State Machine.
 */
public enum WorkItemEvent {
    /**
     * Submit work item for approval.
     */
    SUBMIT,

    /**
     * Start review process (typically called by workflow engine).
     */
    START_REVIEW,

    /**
     * Approve work item.
     */
    APPROVE,

    /**
     * Reject work item.
     */
    REJECT,

    /**
     * Send work item back for rework.
     */
    SEND_TO_REWORK,

    /**
     * Archive approved/rejected work item.
     */
    ARCHIVE,

    /**
     * Cancel work item.
     */
    CANCEL
}
