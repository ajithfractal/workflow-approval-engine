package com.fractalhive.workflowcore.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for registering a new application.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationRegistrationRequest {

    @NotBlank(message = "Application name cannot be blank")
    private String applicationName;

    @NotBlank(message = "Application code cannot be blank")
    private String applicationCode;

    /**
     * API endpoints as key-value pairs.
     * At minimum should contain "userApi" key.
     * Example: {"userApi": "https://app1.example.com/api/users", "notificationApi": "https://app1.example.com/api/notifications"}
     */
    @NotNull(message = "API endpoints cannot be null")
    @NotEmpty(message = "API endpoints cannot be empty")
    private Map<String, Object> apiEndpoints;

    /**
     * Optional API key for calling external APIs.
     * If not provided, a UUID will be generated.
     */
    private String apiKey;
}
