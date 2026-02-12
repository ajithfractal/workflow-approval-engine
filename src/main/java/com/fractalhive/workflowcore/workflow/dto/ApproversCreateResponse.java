package com.fractalhive.workflowcore.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for creating multiple approvers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproversCreateResponse {
    private List<UUID> approverIds;
    private String message;
    private int count;
}
