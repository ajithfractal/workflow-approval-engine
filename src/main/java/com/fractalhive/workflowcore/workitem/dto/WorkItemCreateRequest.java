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

    private String type;

    /**
     * Optional content reference (file path, blob reference, etc.).
     */
    private String contentRef;
}
