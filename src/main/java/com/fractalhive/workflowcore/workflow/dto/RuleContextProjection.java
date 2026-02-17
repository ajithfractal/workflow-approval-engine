package com.fractalhive.workflowcore.workflow.dto;

import java.util.UUID;

public interface RuleContextProjection {

    UUID getWorkflowInstanceId();
    UUID getWorkItemId();
    Integer getVersion();
    Boolean getIsActive();
    String getVariables();
}
