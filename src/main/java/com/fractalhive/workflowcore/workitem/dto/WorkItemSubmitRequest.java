package com.fractalhive.workflowcore.workitem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for submitting a work item.
 * If workItemId is not provided, a new WorkItem will be created automatically.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkItemSubmitRequest {

    /**
     * Optional work item ID. If not provided, a new WorkItem will be created.
     */
    private UUID workItemId;

    /**
     * Work item type (required if workItemId is not provided).
     * Examples: "PURCHASE_ORDER", "CONTRACT", "EXPENSE_CLAIM", etc.
     */
    private String type;

    @NotBlank(message = "Content reference cannot be blank")
    private String contentRef;

    /**
     * Optional custom variables for rule evaluation.
     * Examples: {"amount": 50000, "riskScore": 75, "templateId": "TEMPLATE_001"}
     */
    private Map<String, Object> variables;
}
