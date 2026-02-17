package com.fractalhive.workflowcore.rulesengine.dto;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * DTO for optimized rule context data fetched via native query.
 * Contains only essential fields needed for rule evaluation.
 * Variables are the primary focus - other fields are optional.
 * Variables come as JSON string from native query and will be parsed to Map.
 */
public interface RuleContextData {
    // Variables (primary data for rules - this is what rules are written around)
    // Stored as JSON string in native query result, will be parsed to Map<String, Object>
    String getVariables();
    
    // Task fields (for task-level rules, nullable)
    UUID getTaskId();
    String getApproverId();
    String getApproverType();
    String getTaskStatus();
    Timestamp getTaskDueAt();
    Timestamp getTaskActedAt();
    Timestamp getTaskCreatedAt();
    
    // Step fields (minimal, nullable)
    UUID getStepInstanceId();
    UUID getStepId();
    String getStepStatus();
    
    // Workflow fields (minimal, nullable)
    UUID getWorkflowInstanceId();
    String getWorkflowStatus();
    UUID getWorkflowId();
    Integer getWorkflowVersion();
    Timestamp getWorkflowStartedAt();
    Timestamp getWorkflowCompletedAt();
    
    // Work item fields (minimal, nullable)
    UUID getWorkItemId();
    String getWorkItemType();
    String getWorkItemStatus();
    Integer getWorkItemCurrentVersion();
    String getWorkItemContentRef();
}
