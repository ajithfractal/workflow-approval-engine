package com.fractalhive.workflowcore.workitem.dto;

import com.fractalhive.workflowcore.workitem.enums.WorkItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for work item with version details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkItemResponse {

    private UUID workItemId;
    private String type;
    private WorkItemStatus status;
    private Integer currentVersion;
    private Timestamp createdAt;
    private String createdBy;
    private WorkItemVersionResponse latestVersion;
    private List<WorkItemVersionResponse> versions;
}
