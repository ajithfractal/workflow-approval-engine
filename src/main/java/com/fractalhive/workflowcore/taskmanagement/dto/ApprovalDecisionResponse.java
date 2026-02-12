package com.fractalhive.workflowcore.taskmanagement.dto;

import com.fractalhive.workflowcore.approval.enums.DecisionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Response DTO for approval decision details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDecisionResponse {

    private UUID decisionId;
    private UUID approvalTaskId;
    private DecisionType decision;
    private String comments;
    private String decidedBy;
    private Timestamp decidedAt;
}
