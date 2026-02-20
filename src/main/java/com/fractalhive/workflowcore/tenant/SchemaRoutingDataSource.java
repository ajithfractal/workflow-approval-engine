package com.fractalhive.workflowcore.tenant;

import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Hibernate multi-tenant connection provider that sets PostgreSQL search_path per tenant.
 * Routes queries to the correct schema by setting search_path on each connection.
 *
 * The consuming application must configure Hibernate with:
 * - hibernate.multiTenancy = SCHEMA
 * - hibernate.multi_tenant_connection_provider = this bean
 * - hibernate.tenant_identifier_resolver = TenantIdentifierResolver bean
 */
@Component
public class SchemaRoutingDataSource extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {

    private static final Logger log = LoggerFactory.getLogger(SchemaRoutingDataSource.class);
    public static final String DEFAULT_SCHEMA = "workflow_master";

    private final DataSource dataSource;

    public SchemaRoutingDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected DataSource selectAnyDataSource() {
        return dataSource;
    }

    @Override
    protected DataSource selectDataSource(String tenantIdentifier) {
        // All tenants share the same DataSource — schema is switched via search_path
        return dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return getConnection(DEFAULT_SCHEMA);
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        String schema = sanitizeSchemaName(tenantIdentifier);
        log.debug("Switching to schema: {}", schema);

        Connection connection = dataSource.getConnection();

        try (Statement statement = connection.createStatement()) {
            // Sets tenant schema first, falls back to workflow_master for shared tables
            statement.execute("SET search_path TO " + schema + ", " + DEFAULT_SCHEMA);
        } catch (SQLException e) {
            // If schema switching fails, release connection back to pool and rethrow
            connection.close();
            throw new SQLException("Failed to set search_path for schema: " + schema, e);
        }

        return connection;
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        resetAndRelease(connection, DEFAULT_SCHEMA);
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        resetAndRelease(connection, tenantIdentifier);
    }

    @Override
    public boolean supportsAggressiveRelease() {
        // Return false — we don't want Hibernate aggressively releasing
        // connections back to pool between transactions in the same request
        return false;
    }

    // ─── Private Helpers ────────────────────────────────────────────────────────

    /**
     * Resets search_path back to default before returning connection to HikariCP pool.
     * This is critical — HikariCP reuses connections, so we must clean up the schema
     * context to avoid tenant data leaking into the next request that picks up this connection.
     */
    private void resetAndRelease(Connection connection, String tenantIdentifier) throws SQLException {
        log.debug("Releasing connection for schema: {}", tenantIdentifier);

        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + DEFAULT_SCHEMA);
        } catch (SQLException e) {
            log.warn("Failed to reset search_path on connection release — closing connection instead", e);
            // If we can't reset, close it properly rather than returning a dirty connection to pool
            connection.close();
            return;
        }

        // Return connection to HikariCP pool (does NOT actually close the physical connection)
        connection.close();
    }

    /**
     * Sanitizes schema name to prevent SQL injection.
     * Only allows alphanumeric characters and underscores.
     */
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