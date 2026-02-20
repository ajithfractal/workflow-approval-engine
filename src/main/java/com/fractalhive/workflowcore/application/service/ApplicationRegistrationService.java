package com.fractalhive.workflowcore.application.service;

import com.fractalhive.workflowcore.application.dto.ApplicationRegistrationRequest;
import com.fractalhive.workflowcore.application.dto.ApplicationRegistrationResponse;
import com.fractalhive.workflowcore.application.entity.RegisteredApplication;
import com.fractalhive.workflowcore.application.repository.RegisteredApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing application registration.
 */
@Service
public class ApplicationRegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationRegistrationService.class);

    private final RegisteredApplicationRepository applicationRepository;
    private final SchemaManagementService schemaManagementService;

    public ApplicationRegistrationService(
            RegisteredApplicationRepository applicationRepository,
            SchemaManagementService schemaManagementService) {
        this.applicationRepository = applicationRepository;
        this.schemaManagementService = schemaManagementService;
    }

    /**
     * Registers a new application.
     * Creates a new schema and stores application configuration.
     *
     * @param request   the registration request
     * @param createdBy the user creating the application
     * @return the created application ID
     * @throws IllegalArgumentException if application code already exists or userApi endpoint is missing
     */
    @Transactional
    public UUID registerApplication(ApplicationRegistrationRequest request, String createdBy) {
        // Ensure master schema exists (should already exist from SchemaInitializer, but double-check)
        schemaManagementService.ensureMasterSchemaExists();

        // Validate userApi endpoint exists
        Map<String, String> apiEndpoints = request.getApiEndpoints();
        if (apiEndpoints == null || !apiEndpoints.containsKey("userApi") || apiEndpoints.get("userApi") == null) {
            throw new IllegalArgumentException("API endpoints must contain 'userApi' key");
        }

        // Check if application code already exists
        if (applicationRepository.findByApplicationCode(request.getApplicationCode()).isPresent()) {
            throw new IllegalArgumentException("Application code already exists: " + request.getApplicationCode());
        }

        // Generate schema name
        String schemaName = schemaManagementService.generateSchemaName(request.getApplicationCode());

        // Check if schema name already exists
        if (applicationRepository.findBySchemaName(schemaName).isPresent()) {
            throw new IllegalArgumentException("Schema name already exists: " + schemaName);
        }

        // Create schema
        schemaManagementService.createSchema(schemaName);

        // Generate API key if not provided
        String apiKey = request.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = UUID.randomUUID().toString();
        }

        // Create application entity
        RegisteredApplication application = new RegisteredApplication();
        application.setApplicationName(request.getApplicationName());
        application.setApplicationCode(request.getApplicationCode());
        application.setApiEndpoints(apiEndpoints);
        application.setApiKey(apiKey);
        application.setSchemaName(schemaName);
        application.setActive(true);

        Timestamp now = Timestamp.from(Instant.now());
        application.setCreatedAt(now);
        application.setCreatedBy(createdBy);

        RegisteredApplication saved = applicationRepository.save(application);
        logger.info("Registered application: {} (ID: {}, Schema: {})", request.getApplicationName(), saved.getId(), schemaName);

        return saved.getId();
    }

    /**
     * Gets an application by API key.
     *
     * @param apiKey the API key
     * @return the application if found
     * @throws IllegalArgumentException if application not found
     */
    @Transactional(readOnly = true)
    public RegisteredApplication getApplicationByApiKey(String apiKey) {
        return applicationRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new IllegalArgumentException("Application not found for API key"));
    }

    /**
     * Gets an application by schema name.
     *
     * @param schemaName the schema name
     * @return the application if found
     * @throws IllegalArgumentException if application not found
     */
    @Transactional(readOnly = true)
    public RegisteredApplication getApplicationBySchemaName(String schemaName) {
        return applicationRepository.findBySchemaName(schemaName)
                .orElseThrow(() -> new IllegalArgumentException("Application not found for schema: " + schemaName));
    }

    /**
     * Gets an application by ID.
     *
     * @param appId the application ID
     * @return the application response
     * @throws IllegalArgumentException if application not found
     */
    @Transactional(readOnly = true)
    public ApplicationRegistrationResponse getApplication(UUID appId) {
        RegisteredApplication application = applicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + appId));

        return toResponse(application);
    }

    /**
     * Lists all registered applications.
     *
     * @return list of application responses
     */
    @Transactional(readOnly = true)
    public java.util.List<ApplicationRegistrationResponse> listApplications() {
        return applicationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Lists all active applications.
     *
     * @return list of active application responses
     */
    @Transactional(readOnly = true)
    public java.util.List<ApplicationRegistrationResponse> listActiveApplications() {
        return applicationRepository.findAllByActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Updates an application.
     *
     * @param appId     the application ID
     * @param request   the update request
     * @param updatedBy the user updating the application
     * @throws IllegalArgumentException if application not found or userApi endpoint is missing
     */
    @Transactional
    public void updateApplication(UUID appId, ApplicationRegistrationRequest request, String updatedBy) {
        RegisteredApplication application = applicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + appId));

        // Validate userApi endpoint exists
        Map<String, String> apiEndpoints = request.getApiEndpoints();
        if (apiEndpoints == null || !apiEndpoints.containsKey("userApi") || apiEndpoints.get("userApi") == null) {
            throw new IllegalArgumentException("API endpoints must contain 'userApi' key");
        }

        // Update fields
        application.setApplicationName(request.getApplicationName());
        application.setApiEndpoints(apiEndpoints);
        if (request.getApiKey() != null && !request.getApiKey().trim().isEmpty()) {
            application.setApiKey(request.getApiKey());
        }

        application.setUpdatedAt(Timestamp.from(Instant.now()));
        application.setUpdatedBy(updatedBy);

        applicationRepository.save(application);
        logger.info("Updated application: {} (ID: {})", request.getApplicationName(), appId);
    }

    /**
     * Deactivates an application.
     *
     * @param appId     the application ID
     * @param updatedBy the user deactivating the application
     * @throws IllegalArgumentException if application not found
     */
    @Transactional
    public void deactivateApplication(UUID appId, String updatedBy) {
        RegisteredApplication application = applicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + appId));

        application.setActive(false);
        application.setUpdatedAt(Timestamp.from(Instant.now()));
        application.setUpdatedBy(updatedBy);

        applicationRepository.save(application);
        logger.info("Deactivated application: {} (ID: {})", application.getApplicationName(), appId);
    }

    private ApplicationRegistrationResponse toResponse(RegisteredApplication application) {
        return ApplicationRegistrationResponse.builder()
                .applicationId(application.getId())
                .applicationName(application.getApplicationName())
                .applicationCode(application.getApplicationCode())
                .schemaName(application.getSchemaName())
                .apiEndpoints(application.getApiEndpoints())
                .apiKey(application.getApiKey())
                .active(application.getActive())
                .build();
    }
}
