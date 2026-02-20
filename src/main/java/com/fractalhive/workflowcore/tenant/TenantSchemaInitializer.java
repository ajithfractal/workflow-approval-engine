package com.fractalhive.workflowcore.tenant;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantSchemaInitializer {

    private final DataSource dataSource;
    private final EntityManagerFactory entityManagerFactory;
    
    public static final String DEFAULT_SCHEMA = "workflow_master";

    public void initializeSchema(String schemaName) {
        String sanitized = sanitizeSchemaName(schemaName);
        log.info("Initializing tables for new tenant schema: {}", sanitized);

        try (Connection connection = dataSource.getConnection()) {

            createSchemaIfNotExists(connection, sanitized);

            setSearchPath(connection, sanitized);

            createTablesInSchema(sanitized);

            log.info("Schema '{}' initialized successfully", sanitized);

        } catch (SQLException e) {
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

    private void createTablesInSchema(String schemaName) {
        // Unwrap Hibernate SessionFactory to get its service registry and settings
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);

        // Copy existing Hibernate properties and override schema
        Map<String, Object> settings = new HashMap<>(
                sessionFactory.getProperties()
        );
        settings.put("hibernate.default_schema", schemaName);
        settings.put("hibernate.ddl-auto", "update");

        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(settings)
                .build();

        try {
            MetadataSources metadataSources = new MetadataSources(serviceRegistry);
            registerEntities(metadataSources);

            Metadata metadata = metadataSources.buildMetadata();

            // Hibernate 6 way — use SchemaManager directly
            metadata.buildSessionFactory()
                    .getSchemaManager()
                    .exportMappedObjects(true); // true = create if not exists

            log.debug("Tables created in schema: {}", schemaName);

        } finally {
            StandardServiceRegistryBuilder.destroy(serviceRegistry);
        }
    }

	private void registerEntities(MetadataSources metadataSources) {
		metadataSources.addPackage("com.fractalhive.workflowcore.workflow.entity");
		metadataSources.addPackage("com.fractalhive.workflowcore.approval.entity");
		metadataSources.addPackage("com.fractalhive.workflowcore.workitem.entity");
		metadataSources.addPackage("com.fractalhive.workflowcore.rulesengine.entity");
		metadataSources.addPackage("com.fractalhive.workflowcore.application.entity");
		metadataSources.addPackage("com.fractalhive.workflowcore.common.entity");
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