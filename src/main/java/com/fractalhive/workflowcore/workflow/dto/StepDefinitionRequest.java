package com.fractalhive.workflowcore.workflow.dto;

import com.fractalhive.workflowcore.approval.enums.ApprovalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a workflow step definition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepDefinitionRequest {

    @NotBlank(message = "Step name cannot be blank")
    private String stepName;

    @NotNull(message = "Approval type cannot be null")
    private ApprovalType approvalType;

    @Positive(message = "Step order must be positive")
    private Integer stepOrder;

    @Positive(message = "Min approvals must be positive")
    private Integer minApprovals;

    @Positive(message = "SLA hours must be positive")
    private Integer slaHours;
}
