package com.fractalhive.workflowcore.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

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
    private static final String MASTER_MIGRATION_FILE_PATH = "db/migration/master_schema_tables.sql";

    private final JdbcTemplate jdbcTemplate;
    private final TenantSchemaInitializer tenantSchemaInitializer;
    private final DataSource dataSource;


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
     * Ensures the workflow_master schema exists and creates the registered_application table.
     * Creates schema and tables if they don't exist.
     */
    @Transactional
    public void ensureMasterSchemaExists() {
        try (Connection connection = dataSource.getConnection()) {
            // Create schema if it doesn't exist (using the same connection)
            if (!schemaExists(MASTER_SCHEMA)) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("CREATE SCHEMA IF NOT EXISTS " + MASTER_SCHEMA);
                    logger.info("Created master schema: {}", MASTER_SCHEMA);
                }
            } else {
                logger.debug("Master schema already exists: {}", MASTER_SCHEMA);
            }

            // Execute master schema migration to create tables
            logger.info("Executing master schema migration to create tables...");
            executeMasterSchemaMigration(connection);
            logger.info("Master schema tables created/verified successfully");
            
        } catch (SQLException | IOException e) {
            logger.error("Failed to initialize master schema and tables", e);
            throw new RuntimeException("Failed to initialize master schema", e);
        }
    }

    /**
     * Executes the master schema migration SQL file to create tables.
     */
    private void executeMasterSchemaMigration(Connection connection) throws SQLException, IOException {
        // Ensure we're not in a transaction that might rollback
        boolean autoCommit = connection.getAutoCommit();
        try {
            // Disable auto-commit to ensure all statements are in one transaction
            connection.setAutoCommit(false);
            
            // Set search path to workflow_master for this connection
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET search_path TO " + MASTER_SCHEMA + ", public");
                logger.info("Set search_path to {}", MASTER_SCHEMA);
            }
            
            String migrationSql = loadMasterMigrationSql();
            logger.info("Loaded migration SQL file, length: {} characters", migrationSql.length());
            
            // Log first 500 chars of SQL to verify it's loaded correctly
            logger.debug("SQL file preview (first 500 chars): {}", migrationSql.substring(0, Math.min(500, migrationSql.length())));
            
            executeSqlScript(connection, migrationSql);
            
            // Commit the transaction
            connection.commit();
            logger.info("Committed master schema migration transaction");
            
            // Verify table was created
            if (tableExists(connection, MASTER_SCHEMA, "registered_application")) {
                logger.info("✓ Verified: registered_application table exists in {}", MASTER_SCHEMA);
            } else {
                logger.error("✗ ERROR: registered_application table was NOT found in {} after migration", MASTER_SCHEMA);
                // Try to list all tables in the schema for debugging
                listTablesInSchema(connection, MASTER_SCHEMA);
            }
        } catch (SQLException | IOException e) {
            logger.error("Error during master schema migration, rolling back transaction", e);
            try {
                connection.rollback();
                logger.error("Transaction rolled back");
            } catch (SQLException rollbackEx) {
                logger.error("Failed to rollback transaction", rollbackEx);
            }
            throw e;
        } finally {
            // Restore original auto-commit setting
            connection.setAutoCommit(autoCommit);
        }
    }
    
    /**
     * Lists all tables in a schema for debugging purposes.
     */
    private void listTablesInSchema(Connection connection, String schemaName) {
        try {
            String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? ORDER BY table_name";
            try (java.sql.PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, schemaName);
                try (java.sql.ResultSet rs = stmt.executeQuery()) {
                    logger.info("Tables found in schema '{}':", schemaName);
                    boolean foundAny = false;
                    while (rs.next()) {
                        logger.info("  - {}", rs.getString("table_name"));
                        foundAny = true;
                    }
                    if (!foundAny) {
                        logger.warn("  (no tables found)");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to list tables in schema {}", schemaName, e);
        }
    }

    /**
     * Loads the master schema migration SQL file from classpath.
     */
    private String loadMasterMigrationSql() throws IOException {
        ClassPathResource resource = new ClassPathResource(MASTER_MIGRATION_FILE_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }

    /**
     * Executes a SQL script by splitting it into individual statements.
     * Handles multi-line statements correctly.
     */
    private void executeSqlScript(Connection connection, String sqlScript) throws SQLException {
        // Remove comments and split by semicolon
        String cleanedScript = sqlScript.replaceAll("--.*", ""); // Remove single-line comments
        String[] statements = cleanedScript.split(";");
        int statementCount = 0;
        int executedCount = 0;
        int skippedCount = 0;
        
        try (Statement stmt = connection.createStatement()) {
            for (String statement : statements) {
                String trimmed = statement.trim().replaceAll("\\s+", " "); // Normalize whitespace
                
                // Skip empty statements
                if (trimmed.isEmpty() || trimmed.matches("^\\s*$")) {
                    skippedCount++;
                    continue;
                }
                
                statementCount++;
                try {
                    logger.debug("Executing statement #{}: {}", statementCount, trimmed.substring(0, Math.min(100, trimmed.length())));
                    stmt.execute(trimmed);
                    executedCount++;
                    
                    String logMessage = trimmed.substring(0, Math.min(150, trimmed.length()));
                    
                    if (trimmed.toUpperCase().startsWith("CREATE TABLE")) {
                        logger.info("✓ Executed CREATE TABLE statement #{}: {}", executedCount, logMessage);
                    } else if (trimmed.toUpperCase().startsWith("CREATE INDEX")) {
                        logger.info("✓ Executed CREATE INDEX statement #{}: {}", executedCount, logMessage);
                    } else if (trimmed.toUpperCase().startsWith("CREATE SCHEMA")) {
                        logger.info("✓ Executed CREATE SCHEMA statement #{}: {}", executedCount, logMessage);
                    } else if (trimmed.toUpperCase().startsWith("SET ")) {
                        logger.info("✓ Executed SET statement #{}: {}", executedCount, logMessage);
                    } else {
                        logger.debug("✓ Executed SQL statement #{}: {}", executedCount, logMessage);
                    }
                } catch (SQLException e) {
                    logger.error("✗ Failed to execute SQL statement #{}", statementCount);
                    logger.error("✗ SQL Error Code: {}, SQL State: {}", e.getErrorCode(), e.getSQLState());
                    logger.error("✗ Error Message: {}", e.getMessage());
                    logger.error("✗ SQL statement (first 300 chars): {}", trimmed.substring(0, Math.min(300, trimmed.length())));
                    if (trimmed.length() > 300) {
                        logger.error("✗ SQL statement (remaining): {}", trimmed.substring(300));
                    }
                    throw e;
                }
            }
            
            logger.info("SQL execution summary: {} statements processed, {} executed, {} skipped", 
                    statementCount, executedCount, skippedCount);
        }
    }

    /**
     * Checks if a table exists in a schema.
     */
    private boolean tableExists(Connection connection, String schemaName, String tableName) throws SQLException {
        String sql = "SELECT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = ? AND table_name = ?)";
        try (java.sql.PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        }
        return false;
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
