package com.fractalhive.workflowcore.workflow.enums;

/**
 * Status of a workflow stage instance execution.
 */
public enum StageStatus {
    /**
     * Stage has not been started yet.
     */
    NOT_STARTED,

    /**
     * Stage is currently in progress, waiting for step completions.
     */
    IN_PROGRESS,

    /**
     * Stage completed successfully (all required steps completed).
     */
    COMPLETED,

    /**
     * Stage failed (e.g., step failure, unrecoverable error).
     */
    FAILED
}
