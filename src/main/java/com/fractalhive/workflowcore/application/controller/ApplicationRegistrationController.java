package com.fractalhive.workflowcore.application.controller;

import com.fractalhive.workflowcore.application.dto.ApplicationRegistrationRequest;
import com.fractalhive.workflowcore.application.dto.ApplicationRegistrationResponse;
import com.fractalhive.workflowcore.application.service.ApplicationRegistrationService;
import com.fractalhive.keycloak.util.SecurityUtils;
import com.fractalhive.workflowcore.workflow.dto.CreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for application registration management.
 */
@RestController
@RequestMapping("/api/applications")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@Tag(name = "Application Registration", description = "APIs for registering and managing applications")
public class ApplicationRegistrationController {

    private final ApplicationRegistrationService registrationService;

    public ApplicationRegistrationController(ApplicationRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /**
     * Registers a new application.
     *
     * @param request   the registration request
     * @return the created application ID
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    @Operation(
            summary = "Register new application",
            description = "Registers a new application and creates a dedicated schema for data isolation. Username is extracted from JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Application registered successfully",
                    content = @Content(schema = @Schema(implementation = CreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or duplicate application code")
    })
    public ResponseEntity<CreateResponse> registerApplication(
            @Parameter(description = "Application registration request")
            @Valid @RequestBody ApplicationRegistrationRequest request) {
        String createdBy = SecurityUtils.getCurrentUsername();
        UUID appId = registrationService.registerApplication(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CreateResponse.builder()
                        .id(appId)
                        .message("Application registered successfully")
                        .build());
    }

    /**
     * Lists all registered applications.
     *
     * @return list of applications
     */
    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    @GetMapping
    @Operation(
            summary = "List all applications",
            description = "Retrieves all registered applications"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved applications")
    })
    public ResponseEntity<List<ApplicationRegistrationResponse>> listApplications() {
        List<ApplicationRegistrationResponse> applications = registrationService.listApplications();
        return ResponseEntity.ok(applications);
    }

    /**
     * Lists all active applications.
     *
     * @return list of active applications
     */
    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    @GetMapping("/active")
    @Operation(
            summary = "List active applications",
            description = "Retrieves all active registered applications"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved active applications")
    })
    public ResponseEntity<List<ApplicationRegistrationResponse>> listActiveApplications() {
        List<ApplicationRegistrationResponse> applications = registrationService.listActiveApplications();
        return ResponseEntity.ok(applications);
    }

    /**
     * Gets an application by ID.
     *
     * @param appId the application ID
     * @return the application details
     */
    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    @GetMapping("/{appId}")
    @Operation(
            summary = "Get application by ID",
            description = "Retrieves detailed information about a specific application"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application found",
                    content = @Content(schema = @Schema(implementation = ApplicationRegistrationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    public ResponseEntity<ApplicationRegistrationResponse> getApplication(
            @Parameter(description = "The application ID", required = true)
            @PathVariable UUID appId) {
        ApplicationRegistrationResponse application = registrationService.getApplication(appId);
        return ResponseEntity.ok(application);
    }

    /**
     * Updates an application.
     *
     * @param appId     the application ID
     * @param request   the update request
     * @return success response
     */
    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    @PutMapping("/{appId}")
    @Operation(
            summary = "Update application",
            description = "Updates an existing application's configuration. Username is extracted from JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application updated successfully"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    public ResponseEntity<CreateResponse> updateApplication(
            @Parameter(description = "The application ID", required = true)
            @PathVariable UUID appId,
            @Parameter(description = "Application update request")
            @Valid @RequestBody ApplicationRegistrationRequest request) {
        String updatedBy = SecurityUtils.getCurrentUsername();
        registrationService.updateApplication(appId, request, updatedBy);
        return ResponseEntity.ok(CreateResponse.builder()
                .id(appId)
                .message("Application updated successfully")
                .build());
    }

    /**
     * Deactivates an application.
     *
     * @param appId     the application ID
     * @return success response
     */
    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    @DeleteMapping("/{appId}")
    @Operation(
            summary = "Deactivate application",
            description = "Deactivates an application (does not delete schema or data). Username is extracted from JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    public ResponseEntity<CreateResponse> deactivateApplication(
            @Parameter(description = "The application ID", required = true)
            @PathVariable UUID appId) {
        String updatedBy = SecurityUtils.getCurrentUsername();
        registrationService.deactivateApplication(appId, updatedBy);
        return ResponseEntity.ok(CreateResponse.builder()
                .id(appId)
                .message("Application deactivated successfully")
                .build());
    }
}
