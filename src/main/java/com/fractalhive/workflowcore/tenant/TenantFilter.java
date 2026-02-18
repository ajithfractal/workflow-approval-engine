package com.fractalhive.workflowcore.tenant;

import com.fractalhive.workflowcore.application.repository.RegisteredApplicationRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that extracts schema name from request header/attribute and validates it.
 * The schema name is expected to be set by external Keycloak dependency.
 * Stores schema name in request attribute for SchemaRoutingDataSource.
 */
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TenantFilter.class);

    private static final String SCHEMA_NAME_HEADER = "X-Schema-Name";
    private static final String SCHEMA_NAME_ATTRIBUTE = "schemaName";
    private static final String TENANT_SCHEMA_ATTRIBUTE = "tenantSchema";

    private final RegisteredApplicationRepository applicationRepository;

    public TenantFilter(RegisteredApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Check if user is admin (set by AdminAuthorizationFilter)
        boolean isAdmin = AdminAuthorizationFilter.isAdmin(request);

        // Admin endpoints: allow admins without schema requirement
        if (isAdmin && isAdminEndpoint(path)) {
            // Admins can optionally override schema via header for admin operations
            String adminSchemaOverride = AdminAuthorizationFilter.getAdminSchemaOverride(request);
            if (adminSchemaOverride != null) {
                // Validate admin schema override exists
                boolean schemaExists = applicationRepository.findBySchemaName(adminSchemaOverride).isPresent();
                if (schemaExists) {
                    request.setAttribute(TENANT_SCHEMA_ATTRIBUTE, adminSchemaOverride);
                    logger.debug("Admin using schema override: {} for path: {}", adminSchemaOverride, path);
                } else {
                    logger.warn("Admin schema override invalid: {} for path: {}", adminSchemaOverride, path);
                }
            }
            // Admins can proceed without schema for admin endpoints
            filterChain.doFilter(request, response);
            return;
        }

        // Skip filter for registration endpoint (no schema needed)
        if (path.startsWith("/api/applications/register")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Try to get schema name from header first, then from attribute
        String schemaName = getSchemaName(request);

        if (schemaName == null || schemaName.trim().isEmpty()) {
            logger.warn("Schema name not found in request header or attribute for path: {}", path);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Schema name is required\"}");
            return;
        }

        // Validate schema exists
        boolean schemaExists = applicationRepository.findBySchemaName(schemaName).isPresent();
        if (!schemaExists) {
            logger.warn("Invalid schema name: {} for path: {}", schemaName, path);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Invalid schema name\"}");
            return;
        }

        // Store schema name in request attribute for SchemaRoutingDataSource
        request.setAttribute(TENANT_SCHEMA_ATTRIBUTE, schemaName);
        logger.debug("Set tenant schema: {} for path: {}", schemaName, path);

        filterChain.doFilter(request, response);
    }

    /**
     * Checks if the path is an admin-only endpoint.
     *
     * @param path the request path
     * @return true if admin endpoint
     */
    private boolean isAdminEndpoint(String path) {
        return path.startsWith("/api/applications") && !path.equals("/api/applications/register");
    }

    /**
     * Gets schema name from request header or attribute.
     *
     * @param request the HTTP request
     * @return schema name or null if not found
     */
    private String getSchemaName(HttpServletRequest request) {
        // Try header first
        String schemaName = request.getHeader(SCHEMA_NAME_HEADER);
        if (schemaName != null && !schemaName.trim().isEmpty()) {
            return schemaName.trim();
        }

        // Try request attribute (set by external Keycloak dependency)
        Object attribute = request.getAttribute(SCHEMA_NAME_ATTRIBUTE);
        if (attribute != null) {
            return attribute.toString().trim();
        }

        return null;
    }
}
