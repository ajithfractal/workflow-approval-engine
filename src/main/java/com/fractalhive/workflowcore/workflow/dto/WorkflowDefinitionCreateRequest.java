package com.fractalhive.workflowcore.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a workflow definition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinitionCreateRequest {

    @NotBlank(message = "Workflow name cannot be blank")
    private String name;

    @NotNull(message = "Version cannot be null")
    @Positive(message = "Version must be positive")
    private Integer version;
}
