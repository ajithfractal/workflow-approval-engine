package com.fractalhive.workflowcore.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for adding a comment to an approval task.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalCommentRequest {

    private UUID approvalTaskId;

    private String comment;

    private String commentedBy;
}
