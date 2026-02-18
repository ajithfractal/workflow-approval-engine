package com.fractalhive.workflowcore.tenant;

import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hibernate multi-tenant connection provider that sets PostgreSQL search_path per tenant.
 * Routes queries to the correct schema by setting search_path on each connection.
 * 
 * Note: This requires Hibernate multi-tenancy configuration in the consuming application.
 * The consuming application must configure Hibernate with:
 * - hibernate.multiTenancy = SCHEMA
 * - hibernate.multi_tenant_connection_provider = this bean
 */
@Component
public class SchemaRoutingDataSource extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {

    private static final String TENANT_SCHEMA_ATTRIBUTE = "tenantSchema";
    private static final String DEFAULT_SCHEMA = "public";

    private final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();
    private DataSource defaultDataSource;

    public SchemaRoutingDataSource(DataSource defaultDataSource) {
        this.defaultDataSource = defaultDataSource;
        this.dataSourceMap.put(DEFAULT_SCHEMA, defaultDataSource);
    }

    @Override
    protected DataSource selectAnyDataSource() {
        return defaultDataSource;
    }

    @Override
    protected DataSource selectDataSource(String tenantIdentifier) {
        // For schema-based multi-tenancy, we use the same DataSource
        // but set search_path per connection
        return defaultDataSource;
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = selectDataSource(tenantIdentifier).getConnection();
        
        // Set PostgreSQL search_path to the tenant schema
        try (Statement statement = connection.createStatement()) {
            String schema = tenantIdentifier != null ? tenantIdentifier : DEFAULT_SCHEMA;
            statement.execute("SET search_path TO " + sanitizeSchemaName(schema) + ", public");
        }
        
        return connection;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return getConnection(DEFAULT_SCHEMA);
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        // Reset search_path before closing
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO public");
        }
        connection.close();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        releaseConnection(DEFAULT_SCHEMA, connection);
    }

    /**
     * Gets current tenant schema from request context.
     *
     * @return schema name or null
     */
    public static String getCurrentTenantSchema() {
        try {
            org.springframework.web.context.request.RequestAttributes requestAttributes =
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();

            if (requestAttributes != null) {
                Object schema = requestAttributes.getAttribute(TENANT_SCHEMA_ATTRIBUTE,
                        org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
                if (schema != null) {
                    return schema.toString();
                }
            }
        } catch (Exception e) {
            // If not in request context, return default
        }
        return DEFAULT_SCHEMA;
    }

    /**
     * Sanitizes schema name to prevent SQL injection.
     */
    private String sanitizeSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            return DEFAULT_SCHEMA;
        }
        // Remove any characters that are not alphanumeric or underscore
        return schemaName.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
