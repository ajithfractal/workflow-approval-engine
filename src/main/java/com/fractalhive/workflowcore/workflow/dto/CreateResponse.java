package com.fractalhive.workflowcore.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for create operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateResponse {
    private UUID id;
    private String message;
}
