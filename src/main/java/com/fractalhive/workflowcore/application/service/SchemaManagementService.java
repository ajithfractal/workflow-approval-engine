package com.fractalhive.workflowcore.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fractalhive.workflowcore.tenant.TenantSchemaInitializer;

import lombok.RequiredArgsConstructor;

/**
 * Service for managing PostgreSQL schemas for multi-tenant isolation.
 */
@Service
@RequiredArgsConstructor
public class SchemaManagementService {

    private static final Logger logger = LoggerFactory.getLogger(SchemaManagementService.class);
    private static final String MASTER_SCHEMA = "workflow_master";

    private final JdbcTemplate jdbcTemplate;
    private final TenantSchemaInitializer tenantSchemaInitializer;


    /**
     * Creates a new PostgreSQL schema and initializes tables in it.
     *
     * @param schemaName the schema name (must be sanitized)
     * @throws IllegalArgumentException if schema already exists
     */
    @Transactional
    public void createSchema(String schemaName) {
        tenantSchemaInitializer.initializeSchema(schemaName);
    }

    /**
     * Drops a PostgreSQL schema.
     * Includes safety checks to prevent accidental deletion.
     *
     * @param schemaName the schema name
     * @param force      if true, drops schema even if it contains objects
     * @throws IllegalArgumentException if schema does not exist
     */
    @Transactional
    public void dropSchema(String schemaName, boolean force) {
        if (!schemaExists(schemaName)) {
            throw new IllegalArgumentException("Schema does not exist: " + schemaName);
        }

        String sanitized = sanitizeSchemaName(schemaName);
        String sql = force
                ? "DROP SCHEMA " + sanitized + " CASCADE"
                : "DROP SCHEMA " + sanitized;

        jdbcTemplate.execute(sql);
        logger.info("Dropped schema: {} (force: {})", schemaName, force);
    }

    /**
     * Checks if a schema exists.
     *
     * @param schemaName the schema name
     * @return true if schema exists, false otherwise
     */
    public boolean schemaExists(String schemaName) {
        String sql = "SELECT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)";
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, schemaName);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Sanitizes a schema name to prevent SQL injection.
     * Only allows alphanumeric characters and underscores.
     *
     * @param schemaName the schema name to sanitize
     * @return sanitized schema name
     */
    public String sanitizeSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema name cannot be null or empty");
        }

        // Remove any characters that are not alphanumeric or underscore
        String sanitized = schemaName.replaceAll("[^a-zA-Z0-9_]", "_");

        // Ensure it doesn't start with a number
        if (Character.isDigit(sanitized.charAt(0))) {
            sanitized = "app_" + sanitized;
        }

        return sanitized;
    }

    /**
     * Generates a schema name from application code.
     * Format: app_{applicationCode} (sanitized)
     *
     * @param applicationCode the application code
     * @return generated schema name
     */
    public String generateSchemaName(String applicationCode) {
        String sanitized = sanitizeSchemaName(applicationCode);
        return "app_" + sanitized.toLowerCase();
    }

    /**
     * Ensures the workflow_master schema exists.
     * Creates it if it doesn't exist.
     */
    @Transactional
    public void ensureMasterSchemaExists() {
        if (!schemaExists(MASTER_SCHEMA)) {
            String sql = "CREATE SCHEMA IF NOT EXISTS " + MASTER_SCHEMA;
            jdbcTemplate.execute(sql);
            logger.info("Created master schema: {}", MASTER_SCHEMA);
        } else {
            logger.debug("Master schema already exists: {}", MASTER_SCHEMA);
        }
    }

    /**
     * Gets the master schema name.
     *
     * @return the master schema name
     */
    public String getMasterSchema() {
        return MASTER_SCHEMA;
    }
  
}
