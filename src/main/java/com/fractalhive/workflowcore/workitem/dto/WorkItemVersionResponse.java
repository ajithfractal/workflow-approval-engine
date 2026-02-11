package com.fractalhive.workflowcore.workitem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Response DTO for work item version.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkItemVersionResponse {

    private UUID versionId;
    private UUID workItemId;
    private Integer version;
    private String contentRef;
    private String submittedBy;
    private Timestamp submittedAt;
    private Timestamp createdAt;
    private String createdBy;
}
