package com.fractalhive.workflowcore.taskmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for adding a comment to a task.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCommentRequest {

    /**
     * The comment text.
     */
    @NotBlank(message = "Comment is required")
    private String comment;
}
