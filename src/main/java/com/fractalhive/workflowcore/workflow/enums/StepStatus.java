package com.fractalhive.workflowcore.workflow.enums;

/**
 * Status of a workflow step instance execution.
 */
public enum StepStatus {
    /**
     * Step has not been started yet.
     */
    NOT_STARTED,

    /**
     * Step is currently in progress, waiting for approvals.
     */
    IN_PROGRESS,

    /**
     * Step completed successfully (approval criteria met).
     */
    COMPLETED,

    /**
     * Step failed (e.g., SLA breach, unrecoverable error).
     */
    FAILED
}
