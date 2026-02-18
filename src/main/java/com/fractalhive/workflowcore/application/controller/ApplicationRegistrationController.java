package com.fractalhive.workflowcore.application.controller;

import com.fractalhive.workflowcore.application.dto.ApplicationRegistrationRequest;
import com.fractalhive.workflowcore.application.dto.ApplicationRegistrationResponse;
import com.fractalhive.workflowcore.application.service.ApplicationRegistrationService;
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
     * @param createdBy the user creating the application
     * @return the created application ID
     */
    @PostMapping("/register")
    @Operation(
            summary = "Register new application",
            description = "Registers a new application and creates a dedicated schema for data isolation"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Application registered successfully",
                    content = @Content(schema = @Schema(implementation = CreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or duplicate application code")
    })
    public ResponseEntity<CreateResponse> registerApplication(
            @Parameter(description = "Application registration request")
            @Valid @RequestBody ApplicationRegistrationRequest request,
            @Parameter(description = "User ID creating the application", required = true, example = "admin")
            @RequestParam String createdBy) {
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
     * @param updatedBy the user updating the application
     * @return success response
     */
    @PutMapping("/{appId}")
    @Operation(
            summary = "Update application",
            description = "Updates an existing application's configuration"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application updated successfully"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    public ResponseEntity<CreateResponse> updateApplication(
            @Parameter(description = "The application ID", required = true)
            @PathVariable UUID appId,
            @Parameter(description = "Application update request")
            @Valid @RequestBody ApplicationRegistrationRequest request,
            @Parameter(description = "User ID updating the application", required = true, example = "admin")
            @RequestParam String updatedBy) {
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
     * @param updatedBy the user deactivating the application
     * @return success response
     */
    @DeleteMapping("/{appId}")
    @Operation(
            summary = "Deactivate application",
            description = "Deactivates an application (does not delete schema or data)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    public ResponseEntity<CreateResponse> deactivateApplication(
            @Parameter(description = "The application ID", required = true)
            @PathVariable UUID appId,
            @Parameter(description = "User ID deactivating the application", required = true, example = "admin")
            @RequestParam String updatedBy) {
        registrationService.deactivateApplication(appId, updatedBy);
        return ResponseEntity.ok(CreateResponse.builder()
                .id(appId)
                .message("Application deactivated successfully")
                .build());
    }
}
