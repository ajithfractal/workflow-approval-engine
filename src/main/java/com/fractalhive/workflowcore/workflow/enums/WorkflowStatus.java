package com.fractalhive.workflowcore.workflow.enums;

/**
 * Status of a workflow instance execution.
 */
public enum WorkflowStatus {
    /**
     * Workflow instance has been created but not yet started.
     */
    NOT_STARTED,

    /**
     * Workflow instance is currently executing.
     */
    IN_PROGRESS,

    /**
     * Workflow instance completed successfully.
     */
    COMPLETED,

    /**
     * Workflow instance failed or was terminated.
     */
    FAILED,

    /**
     * Workflow instance was cancelled.
     */
    CANCELLED
}
