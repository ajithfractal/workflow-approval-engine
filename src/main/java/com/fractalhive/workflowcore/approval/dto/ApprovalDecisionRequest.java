package com.fractalhive.workflowcore.approval.dto;

import com.fractalhive.workflowcore.approval.enums.DecisionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for recording an approval decision.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDecisionRequest {

    private UUID approvalTaskId;

    private DecisionType decision;

    private String comments;

    private String decidedBy;
}
