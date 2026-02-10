package com.fractalhive.workflowcore.approval.enums;

/**
 * Defines the approval rule type for a workflow step.
 */
public enum ApprovalType {
    /**
     * All assigned approvers must approve before the step completes.
     */
    ALL,

    /**
     * Any one approval is sufficient to complete the step.
     */
    ANY,

    /**
     * Requires a minimum number (N) of approvals out of total assigned approvers (M).
     */
    N_OF_M
}
