package com.fractalhive.workflowcore.workflow.dto;

import com.fractalhive.workflowcore.approval.enums.ApproverType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding an approver to a workflow step.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproverRequest {

    @NotNull(message = "Approver type cannot be null")
    private ApproverType approverType;

    @NotBlank(message = "Approver value cannot be blank")
    private String approverValue;
}
