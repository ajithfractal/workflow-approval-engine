package com.fractalhive.workflowcore.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for application registration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationRegistrationResponse {

    private UUID applicationId;
    private String applicationName;
    private String applicationCode;
    private String schemaName;
    private Map<String, String> apiEndpoints;
    private String apiKey;
    private Boolean active;
}
