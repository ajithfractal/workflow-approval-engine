package com.fractalhive.workflowcore.rulesengine.dto;

import com.fractalhive.workflowcore.approval.enums.ApproverType;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import com.fractalhive.workflowcore.workflow.enums.StepStatus;
import com.fractalhive.workflowcore.workflow.enums.WorkflowStatus;
import com.fractalhive.workflowcore.workitem.enums.WorkItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

/**
 * Context object containing all data accessible in rule expressions.
 * Contains only flattened, primitive data - no JPA entities.
 * This keeps the rule layer lightweight, deterministic, and independent of the domain model.
 * 
 * PRIMARY FOCUS: Variables - Rules are written around variables (e.g., variables['loanAmount'] <= 30000).
 * Other fields are optional and only included when needed for specific rule types (SLA, status checks, etc.).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleContext {

    /**
     * Client-provided custom variables from WorkItemVersion.
     * This is the PRIMARY data that rules are written around.
     * Accessible in SpEL as: variables['amount'] or variables.get("riskScore")
     * Can also be accessed directly: amount (if key exists in variables map)
     */
    private Map<String, Object> variables;

    // Work Item fields (flattened)
    private UUID workItemId;
    private String workItemType;
    private WorkItemStatus workItemStatus;
    private Integer workItemCurrentVersion;
    private String workItemContentRef; // From active WorkItemVersion

    // Workflow Instance fields (flattened)
    private UUID workflowInstanceId;
    private UUID workflowId;
    private Integer workflowVersion;
    private WorkflowStatus workflowStatus;
    private Timestamp workflowStartedAt;
    private Timestamp workflowCompletedAt;

    // Step Instance fields (flattened)
    private UUID stepInstanceId;
    private UUID stepId;
    private StepStatus stepStatus;
    private Timestamp stepStartedAt;
    private Timestamp stepCompletedAt;

    // Task fields (flattened, if evaluating task-level rules)
    private UUID taskId;
    private String approverId;
    private ApproverType approverType;
    private TaskStatus taskStatus;
    private Timestamp taskDueAt;
    private Timestamp taskActedAt;
    private Timestamp taskCreatedAt;

    // Calculated/derived fields
    /**
     * Elapsed hours since task creation (for SLA evaluation).
     */
    private Long elapsedHours;

    /**
     * Hours until task due date (for SLA evaluation).
     */
    private Long hoursUntilDue;

    /**
     * Whether the current step has parallel steps.
     */
    private Boolean hasParallelSteps;

    /**
     * Number of rejected steps in the parallel group.
     */
    private Integer rejectionCount;
}
