package com.fractalhive.workflowcore.application.config;

import com.fractalhive.workflowcore.application.service.SchemaManagementService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Component that initializes the workflow_master schema on application startup.
 * Ensures the master schema exists before any operations that require it.
 */
@Component
public class SchemaInitializer {

    private static final Logger logger = LoggerFactory.getLogger(SchemaInitializer.class);

    private final SchemaManagementService schemaManagementService;

    public SchemaInitializer(SchemaManagementService schemaManagementService) {
        this.schemaManagementService = schemaManagementService;
    }

    /**
     * Initializes the workflow_master schema on application startup.
     * This ensures the master schema exists before any database operations.
     */
    @PostConstruct
    public void initializeMasterSchema() {
        try {
            logger.info("Initializing workflow_master schema...");
            schemaManagementService.ensureMasterSchemaExists();
            logger.info("Workflow_master schema initialization completed successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize workflow_master schema", e);
            throw new RuntimeException("Failed to initialize workflow_master schema", e);
        }
    }
}
