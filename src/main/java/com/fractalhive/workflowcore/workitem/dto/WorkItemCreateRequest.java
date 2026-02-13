package com.fractalhive.workflowcore.workitem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new work item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkItemCreateRequest {

    /**
     * Type of the work item (e.g., "contract", "purchase_request", etc.).
     */
    private String type;
}
