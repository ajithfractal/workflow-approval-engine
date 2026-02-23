package com.fractalhive.workflowcore.application.controller;

import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.DESC_DEACTIVATE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.DESC_GET_BY_ID;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.DESC_LIST_ACTIVE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.DESC_LIST_ALL;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.DESC_REGISTER;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.DESC_UPDATE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.PARAM_APP_ID;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.PARAM_REGISTRATION_REQUEST;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.PARAM_UPDATE_REQUEST;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.RESP_APPLICATION_FOUND;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.RESP_DEACTIVATED_SUCCESS;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.RESP_INVALID_OR_DUPLICATE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.RESP_REGISTERED_SUCCESS;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.RESP_RETRIEVED_ACTIVE_APPLICATIONS;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.RESP_RETRIEVED_APPLICATIONS;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.RESP_UPDATED_SUCCESS;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.SUMMARY_DEACTIVATE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.SUMMARY_GET_BY_ID;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.SUMMARY_LIST_ACTIVE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.SUMMARY_LIST_ALL;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.SUMMARY_REGISTER;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ApplicationRegistration.SUMMARY_UPDATE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Common.NOT_FOUND;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ResponseCodes.BAD_REQUEST_400;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ResponseCodes.CREATED_201;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ResponseCodes.NOT_FOUND_404;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ResponseCodes.OK_200;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fractalhive.keycloak.util.SecurityUtils;
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
import lombok.RequiredArgsConstructor;

/**
 * REST controller for application registration management.
 */
@RestController
@RequestMapping("/api/applications")
@Tag(name = "Application Registration", description = "APIs for registering and managing applications")
@RequiredArgsConstructor
public class ApplicationRegistrationController {

    private final ApplicationRegistrationService registrationService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    @Operation(
            summary = SUMMARY_REGISTER,
            description = DESC_REGISTER
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = CREATED_201, description = RESP_REGISTERED_SUCCESS,
                    content = @Content(schema = @Schema(implementation = CreateResponse.class))),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = RESP_INVALID_OR_DUPLICATE)
    })
    public ResponseEntity<CreateResponse> registerApplication(
            @Parameter(description = PARAM_REGISTRATION_REQUEST)
            @Valid @RequestBody ApplicationRegistrationRequest request) {
        String createdBy = SecurityUtils.getCurrentUsername();
        UUID appId = registrationService.registerApplication(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CreateResponse.builder()
                        .id(appId)
                        .message("Application registered successfully")
                        .build());
    }

    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    @GetMapping
    @Operation(
            summary = SUMMARY_LIST_ALL,
            description = DESC_LIST_ALL
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_RETRIEVED_APPLICATIONS)
    })
    public List<ApplicationRegistrationResponse> listApplications() {
        return registrationService.listApplications();
    }

    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    @GetMapping("/active")
    @Operation(
            summary = SUMMARY_LIST_ACTIVE,
            description = DESC_LIST_ACTIVE
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_RETRIEVED_ACTIVE_APPLICATIONS)
    })
    public List<ApplicationRegistrationResponse> listActiveApplications() {
        return registrationService.listActiveApplications();
    }

    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    @GetMapping("/{appId}")
    @Operation(
            summary = SUMMARY_GET_BY_ID,
            description = DESC_GET_BY_ID
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_APPLICATION_FOUND,
                    content = @Content(schema = @Schema(implementation = ApplicationRegistrationResponse.class))),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public ApplicationRegistrationResponse getApplication(
            @Parameter(description = PARAM_APP_ID, required = true)
            @PathVariable UUID appId) {
        return registrationService.getApplication(appId);
    }

    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    @PutMapping("/{appId}")
    @Operation(
            summary = SUMMARY_UPDATE,
            description = DESC_UPDATE
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_UPDATED_SUCCESS),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public CreateResponse updateApplication(
            @Parameter(description = PARAM_APP_ID, required = true)
            @PathVariable UUID appId,
            @Parameter(description = PARAM_UPDATE_REQUEST)
            @Valid @RequestBody ApplicationRegistrationRequest request) {
        String updatedBy = SecurityUtils.getCurrentUsername();
        registrationService.updateApplication(appId, request, updatedBy);
        return CreateResponse.builder()
                .id(appId)
                .message("Application updated successfully")
                .build();
    }

    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    @DeleteMapping("/{appId}")
    @Operation(
            summary = SUMMARY_DEACTIVATE,
            description = DESC_DEACTIVATE
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_DEACTIVATED_SUCCESS),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public CreateResponse deactivateApplication(
            @Parameter(description = PARAM_APP_ID, required = true)
            @PathVariable UUID appId) {
        String updatedBy = SecurityUtils.getCurrentUsername();
        registrationService.deactivateApplication(appId, updatedBy);
        return CreateResponse.builder()
                .id(appId)
                .message("Application deactivated successfully")
                .build();
    }
}
