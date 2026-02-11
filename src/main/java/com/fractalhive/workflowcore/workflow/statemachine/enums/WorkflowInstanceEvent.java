package com.fractalhive.workflowcore.workflow.statemachine.enums;

/**
 * Events for the Workflow Instance State Machine.
 */
public enum WorkflowInstanceEvent {
    /**
     * Start workflow execution.
     */
    START,

    /**
     * Complete workflow successfully.
     */
    COMPLETE,

    /**
     * Fail workflow execution.
     */
    FAIL,

    /**
     * Cancel workflow.
     */
    CANCEL
}
