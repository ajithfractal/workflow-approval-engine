package com.fractalhive.workflowcore.workitem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for submitting a work item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkItemSubmitRequest {

    @NotBlank(message = "Content reference cannot be blank")
    private String contentRef;
}
