package com.fractalhive.workflowcore.workitem.enums;

/**
 * Status of a work item lifecycle.
 */
public enum WorkItemStatus {
    /**
     * Work item is in draft state, not yet submitted.
     */
    DRAFT,

    /**
     * Work item has been submitted for approval.
     */
    SUBMITTED,

    /**
     * Work item is currently under review in a workflow.
     */
    IN_REVIEW,

    /**
     * Work item was rejected and sent back for rework.
     */
    REWORK,

    /**
     * Work item has been approved.
     */
    APPROVED,

    /**
     * Work item has been rejected.
     */
    REJECTED,

    /**
     * Work item workflow was cancelled.
     */
    CANCELLED,

    /**
     * Work item has been archived.
     */
    ARCHIVED
}
