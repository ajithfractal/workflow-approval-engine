package com.fractalhive.workflowcore.taskmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Response DTO for approval comment details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalCommentResponse {

    private UUID commentId;
    private UUID approvalTaskId;
    private String comment;
    private String commentedBy;
    private Timestamp commentedAt;
}
