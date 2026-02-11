package com.fractalhive.workflowcore.workflow.statemachine.enums;

/**
 * Events for the Workflow Step Instance State Machine.
 */
public enum WorkflowStepInstanceEvent {
    /**
     * Start step execution.
     */
    START,

    /**
     * Complete step successfully.
     */
    COMPLETE,

    /**
     * Fail step execution.
     */
    FAIL
}
