package com.fractalhive.workflowcore.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for starting a workflow instance for a work item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStartRequest {

    private UUID workItemId;

    private UUID workflowDefinitionId;

    /**
     * Optional workflow version.
     * If null, the latest active version will be used.
     */
    private Integer workflowVersion;
}
