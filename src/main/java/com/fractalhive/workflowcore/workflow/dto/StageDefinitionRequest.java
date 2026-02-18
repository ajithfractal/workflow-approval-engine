package com.fractalhive.workflowcore.workflow.dto;

import com.fractalhive.workflowcore.approval.enums.ApprovalType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a workflow stage definition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageDefinitionRequest {

    @NotBlank(message = "Stage name cannot be blank")
    private String stageName;

    @Positive(message = "Stage order must be positive")
    private Integer stageOrder;

    @NotNull(message = "Step completion type cannot be null")
    private ApprovalType stepCompletionType;

    @Positive(message = "Min step completions must be positive")
    private Integer minStepCompletions;

    /**
     * Optional list of steps to add during stage creation.
     * If provided, steps will be created along with the stage.
     */
    @Valid
    private List<StepDefinitionRequest> steps;
}
