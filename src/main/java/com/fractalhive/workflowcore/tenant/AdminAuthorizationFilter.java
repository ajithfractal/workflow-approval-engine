package com.fractalhive.workflowcore.tenant;

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
 * Filter that checks for admin authorization from Keycloak roles.
 * Sets admin flag in request attribute if user has WORKFLOW_ADMIN role.
 * 
 * Expected Keycloak role: "WORKFLOW_ADMIN"
 */
@Component
@Order(0)
public class AdminAuthorizationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AdminAuthorizationFilter.class);

    private static final String WORKFLOW_ADMIN_ROLE = "WORKFLOW_ADMIN";
    private static final String ROLES_ATTRIBUTE = "roles";
    private static final String IS_ADMIN_ATTRIBUTE = "isWorkflowAdmin";
    private static final String ADMIN_SCHEMA_OVERRIDE_ATTRIBUTE = "adminSchemaOverride";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Check for admin role from Keycloak (set by external Keycloak dependency)
        boolean isAdmin = checkAdminAuthorization(request);

        if (isAdmin) {
            // Set admin flag in request attribute
            request.setAttribute(IS_ADMIN_ATTRIBUTE, true);
            logger.debug("User is workflow engine admin for path: {}", request.getRequestURI());

            // Admins can optionally override schema via header
            String adminSchemaOverride = request.getHeader("X-Admin-Schema-Override");
            if (adminSchemaOverride != null && !adminSchemaOverride.trim().isEmpty()) {
                request.setAttribute(ADMIN_SCHEMA_OVERRIDE_ATTRIBUTE, adminSchemaOverride.trim());
                logger.debug("Admin schema override set to: {}", adminSchemaOverride);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Checks if the user has admin authorization.
     * Looks for WORKFLOW_ADMIN role in request attribute (set by external Keycloak dependency).
     *
     * @param request the HTTP request
     * @return true if user has WORKFLOW_ADMIN role, false otherwise
     */
    private boolean checkAdminAuthorization(HttpServletRequest request) {
        // Check for roles claim (array or string)
        Object roles = request.getAttribute(ROLES_ATTRIBUTE);
        if (roles != null) {
            if (roles instanceof String) {
                return ((String) roles).contains(WORKFLOW_ADMIN_ROLE);
            }
            if (roles instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> rolesList = (java.util.List<String>) roles;
                return rolesList.contains(WORKFLOW_ADMIN_ROLE);
            }
        }

        return false;
    }

    /**
     * Checks if the current request is from an admin user.
     * Can be called from controllers or services.
     *
     * @param request the HTTP request
     * @return true if user is admin
     */
    public static boolean isAdmin(HttpServletRequest request) {
        Object isAdmin = request.getAttribute(IS_ADMIN_ATTRIBUTE);
        return isAdmin != null && (Boolean) isAdmin;
    }

    /**
     * Gets admin schema override if set.
     *
     * @param request the HTTP request
     * @return schema name override or null
     */
    public static String getAdminSchemaOverride(HttpServletRequest request) {
        Object override = request.getAttribute(ADMIN_SCHEMA_OVERRIDE_ATTRIBUTE);
        return override != null ? override.toString() : null;
    }
}
