package com.fractalhive.workflowcore.tenant;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantSchemaInitializer {

    private final DataSource dataSource;
    
    public static final String DEFAULT_SCHEMA = "workflow_master";
    private static final String MIGRATION_FILE_PATH = "db/migration/tenant_schema_tables.sql";
    private static final String SCHEMA_NAME_PLACEHOLDER = "{SCHEMA_NAME}";

    public void initializeSchema(String schemaName) {
        String sanitized = sanitizeSchemaName(schemaName);
        log.info("Initializing tables for new tenant schema: {}", sanitized);

        try (Connection connection = dataSource.getConnection()) {

            createSchemaIfNotExists(connection, sanitized);

            setSearchPath(connection, sanitized);

            createTablesInSchema(connection, sanitized);

            log.info("Schema '{}' initialized successfully", sanitized);

        } catch (SQLException | IOException e) {
            log.error("Failed to initialize schema '{}'", sanitized, e);
            throw new RuntimeException("Schema initialization failed for: " + sanitized, e);
        }
    }

    private void createSchemaIfNotExists(Connection connection, String schema) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
            log.debug("Schema created or already exists: {}", schema);
        }
    }

    private void setSearchPath(Connection connection, String schema) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO " + schema + ", workflow_master");
        }
    }

    private void createTablesInSchema(Connection connection, String schemaName) throws SQLException, IOException {
        // Ensure we're in a transaction
        boolean autoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            
            String migrationSql = loadMigrationSql();
            String sqlWithSchema = migrationSql.replace(SCHEMA_NAME_PLACEHOLDER, schemaName);
            
            setSearchPath(connection, schemaName);
            log.info("Executing tenant schema migration for: {}", schemaName);
            executeSqlScript(connection, sqlWithSchema);
            
            // Commit the transaction
            connection.commit();
            log.info("Committed tenant schema migration transaction for: {}", schemaName);
            
        } catch (SQLException | IOException e) {
            log.error("Error during tenant schema migration for {}, rolling back transaction", schemaName, e);
            try {
                connection.rollback();
                log.error("Transaction rolled back for schema: {}", schemaName);
            } catch (SQLException rollbackEx) {
                log.error("Failed to rollback transaction for schema: {}", schemaName, rollbackEx);
            }
            throw e;
        } finally {
            // Restore original auto-commit setting
            connection.setAutoCommit(autoCommit);
        }
    }

    private String loadMigrationSql() throws IOException {
        ClassPathResource resource = new ClassPathResource(MIGRATION_FILE_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }

    private void executeSqlScript(Connection connection, String sqlScript) throws SQLException {
        // Remove single-line comments (-- to end of line) but preserve the line structure
        String cleanedScript = sqlScript.replaceAll("(?m)^\\s*--.*$", ""); // Remove comment-only lines
        cleanedScript = cleanedScript.replaceAll("--[^\r\n]*", ""); // Remove inline comments
        
        // Split SQL script by semicolon
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
                    log.debug("Executing statement #{}: {}", statementCount, trimmed.substring(0, Math.min(100, trimmed.length())));
                    stmt.execute(trimmed);
                    executedCount++;
                    
                    String logMessage = trimmed.substring(0, Math.min(150, trimmed.length()));
                    if (trimmed.toUpperCase().startsWith("CREATE TABLE")) {
                        log.info("✓ Executed CREATE TABLE statement #{}: {}", executedCount, logMessage);
                    } else if (trimmed.toUpperCase().startsWith("CREATE INDEX")) {
                        log.info("✓ Executed CREATE INDEX statement #{}: {}", executedCount, logMessage);
                    } else if (trimmed.toUpperCase().startsWith("CREATE SCHEMA")) {
                        log.info("✓ Executed CREATE SCHEMA statement #{}: {}", executedCount, logMessage);
                    } else if (trimmed.toUpperCase().startsWith("SET ")) {
                        log.debug("✓ Executed SET statement #{}: {}", executedCount, logMessage);
                    } else {
                        log.debug("✓ Executed SQL statement #{}: {}", executedCount, logMessage);
                    }
                } catch (SQLException e) {
                    log.error("✗ Failed to execute SQL statement #{}", statementCount);
                    log.error("✗ SQL Error Code: {}, SQL State: {}", e.getErrorCode(), e.getSQLState());
                    log.error("✗ Error Message: {}", e.getMessage());
                    log.error("✗ SQL statement (first 300 chars): {}", trimmed.substring(0, Math.min(300, trimmed.length())));
                    if (trimmed.length() > 300) {
                        log.error("✗ SQL statement (remaining): {}", trimmed.substring(300));
                    }
                    throw e;
                }
            }
            
            log.info("SQL execution summary: {} statements processed, {} executed, {} skipped", 
                    statementCount, executedCount, skippedCount);
        }
    }
	
	private String sanitizeSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            log.warn("Null or empty schema name received, falling back to default: {}", DEFAULT_SCHEMA);
            return DEFAULT_SCHEMA;
        }

        String sanitized = schemaName.replaceAll("[^a-zA-Z0-9_]", "_");

        if (!sanitized.equals(schemaName)) {
            log.warn("Schema name '{}' contained invalid characters, sanitized to '{}'", schemaName, sanitized);
        }

        return sanitized;
    }
}