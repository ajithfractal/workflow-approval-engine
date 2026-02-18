package com.fractalhive.workflowcore.workflow.statemachine.enums;

/**
 * Events for the Workflow Stage Instance State Machine.
 */
public enum StageEvent {
    /**
     * Start stage execution.
     */
    START,

    /**
     * Complete stage successfully.
     */
    COMPLETE,

    /**
     * Fail stage execution.
     */
    FAIL
}
