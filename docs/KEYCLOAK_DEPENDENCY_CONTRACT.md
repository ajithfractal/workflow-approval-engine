# Keycloak Dependency Contract

## Overview

This document specifies the contract that `fractalhive-spring-boot-starter-keycloak` must implement to integrate with the workflow engine.

## Required Functionality

The Keycloak dependency must provide a **Servlet Filter** that:

1. **Validates JWT tokens** from incoming HTTP requests
2. **Extracts claims** from validated JWT tokens
3. **Sets request attributes** for downstream filters and services

## Filter Requirements

### 1. Filter Order

The Keycloak filter **MUST** run **BEFORE** the workflow engine filters:

- `AdminAuthorizationFilter` has `@Order(0)`
- `TenantFilter` has `@Order(1)`
- **Keycloak filter should have `@Order(-1)` or lower** to ensure it runs first

### 2. Required Request Attributes

The Keycloak filter **MUST** set the following request attributes:

#### 2.1. Roles Attribute

**Attribute Name:** `roles`

**Type:** `List<String>` or `String`

**Source:** JWT claim `roles` or `realm_access.roles`

**Purpose:** Used by `AdminAuthorizationFilter` to check for `WORKFLOW_ADMIN` role

**Example:**
```java
// Extract from JWT
List<String> roles = jwt.getClaimAsStringList("roles");
// or
String roles = jwt.getClaimAsString("roles");

// Set in request
request.setAttribute("roles", roles);
```

**JWT Token Example:**
```json
{
  "sub": "user-id",
  "roles": ["WORKFLOW_ADMIN", "user"]
}
```

or

```json
{
  "sub": "user-id",
  "realm_access": {
    "roles": ["WORKFLOW_ADMIN", "user"]
  }
}
```

#### 2.2. Schema Name Attribute

**Attribute Name:** `schemaName`

**Type:** `String`

**Source:** JWT claim `schema`

**Purpose:** Used by `TenantFilter` to determine tenant schema for multi-tenancy

**Example:**
```java
// Extract from JWT
String schema = jwt.getClaimAsString("schema");

// Set in request (if present)
if (schema != null && !schema.trim().isEmpty()) {
    request.setAttribute("schemaName", schema.trim());
}
```

**JWT Token Example:**
```json
{
  "sub": "user-id",
  "schema": "app_webapp1",
  "roles": ["user"]
}
```

### 3. Token Validation

The Keycloak filter **MUST**:

- Validate JWT token signature
- Validate token expiration
- Validate token issuer (if configured)
- Reject invalid tokens with appropriate HTTP status (401 Unauthorized)

### 4. Error Handling

The Keycloak filter **SHOULD**:

- Return `401 Unauthorized` for invalid/missing tokens
- Return `403 Forbidden` for expired tokens
- Log authentication failures for security monitoring

## Implementation Example

### Basic Filter Structure

```java
package com.fractalhive.keycloak.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Keycloak authentication filter that validates JWT tokens
 * and sets request attributes for workflow engine integration.
 */
@Component
@Order(-1) // Run before workflow engine filters
public class KeycloakAuthenticationFilter extends OncePerRequestFilter {

    private static final String ROLES_ATTRIBUTE = "roles";
    private static final String SCHEMA_NAME_ATTRIBUTE = "schemaName";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. Extract and validate JWT token from Authorization header
            String token = extractToken(request);
            if (token == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Missing authorization token\"}");
                return;
            }

            // 2. Validate token (signature, expiration, issuer)
            Jwt jwt = validateToken(token);
            if (jwt == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Invalid token\"}");
                return;
            }

            // 3. Extract roles claim
            List<String> roles = extractRoles(jwt);
            if (roles != null && !roles.isEmpty()) {
                request.setAttribute(ROLES_ATTRIBUTE, roles);
            }

            // 4. Extract schema claim
            String schema = extractSchema(jwt);
            if (schema != null && !schema.trim().isEmpty()) {
                request.setAttribute(SCHEMA_NAME_ATTRIBUTE, schema.trim());
            }

            // 5. Continue filter chain
            filterChain.doFilter(request, response);

        } catch (TokenExpiredException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Token expired\"}");
        } catch (Exception e) {
            logger.error("Error processing Keycloak authentication", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Authentication error\"}");
        }
    }

    /**
     * Extracts JWT token from Authorization header.
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * Validates JWT token and returns Jwt object.
     */
    private Jwt validateToken(String token) {
        // Implement token validation logic
        // - Verify signature
        // - Check expiration
        // - Validate issuer
        // Return Jwt object or null if invalid
        return null; // Placeholder
    }

    /**
     * Extracts roles from JWT token.
     * Supports both "roles" and "realm_access.roles" claims.
     */
    private List<String> extractRoles(Jwt jwt) {
        // Try "roles" claim first
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null && !roles.isEmpty()) {
            return roles;
        }

        // Try "realm_access.roles" claim
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> realmRoles = (List<String>) rolesObj;
                return realmRoles;
            }
        }

        return null;
    }

    /**
     * Extracts schema name from JWT token.
     */
    private String extractSchema(Jwt jwt) {
        return jwt.getClaimAsString("schema");
    }
}
```

## Required Methods/API

The Keycloak dependency should expose the following (if needed by consuming applications):

### Configuration Methods

```java
public interface KeycloakConfig {
    /**
     * Configure Keycloak server URL
     */
    void setKeycloakServerUrl(String url);

    /**
     * Configure realm name
     */
    void setRealm(String realm);

    /**
     * Configure client ID
     */
    void setClientId(String clientId);

    /**
     * Configure public key for token validation
     */
    void setPublicKey(String publicKey);
}
```

### Utility Methods (Optional)

```java
public class KeycloakUtils {
    /**
     * Extract roles from HttpServletRequest
     */
    public static List<String> getRoles(HttpServletRequest request);

    /**
     * Extract schema from HttpServletRequest
     */
    public static String getSchema(HttpServletRequest request);

    /**
     * Check if user has specific role
     */
    public static boolean hasRole(HttpServletRequest request, String role);
}
```

## Integration Points

### 1. Request Flow

```
1. HTTP Request arrives
2. Keycloak Filter (Order: -1)
   - Validates JWT token
   - Extracts claims
   - Sets request attributes: "roles", "schemaName"
3. AdminAuthorizationFilter (Order: 0)
   - Reads "roles" attribute
   - Checks for "WORKFLOW_ADMIN" role
   - Sets "isWorkflowAdmin" attribute
4. TenantFilter (Order: 1)
   - Reads "schemaName" attribute
   - Validates schema exists
   - Sets "tenantSchema" attribute
5. SchemaRoutingDataSource
   - Reads "tenantSchema" attribute
   - Routes database queries to correct schema
```

### 2. Skip Paths

The Keycloak filter **MAY** skip authentication for:

- `/api/applications/register` (public endpoint)
- Health check endpoints (`/actuator/health`)
- Swagger/OpenAPI endpoints (`/swagger-ui/**`, `/v3/api-docs/**`)

## Testing Requirements

The Keycloak dependency should be tested with:

1. **Valid tokens** with roles and schema claims
2. **Admin tokens** with `WORKFLOW_ADMIN` role
3. **Tenant tokens** with `schema` claim
4. **Invalid tokens** (malformed, expired, wrong signature)
5. **Missing tokens** (no Authorization header)

## Summary

### Required Request Attributes

| Attribute Name | Type | Source Claim | Required | Purpose |
|----------------|------|--------------|----------|---------|
| `roles` | `List<String>` or `String` | `roles` or `realm_access.roles` | Yes | Admin authorization check |
| `schemaName` | `String` | `schema` | No (for admin users) | Tenant schema routing |

### Filter Order

- Keycloak Filter: `@Order(-1)` or lower
- AdminAuthorizationFilter: `@Order(0)`
- TenantFilter: `@Order(1)`

### Error Responses

- Missing token: `401 Unauthorized`
- Invalid token: `401 Unauthorized`
- Expired token: `403 Forbidden`

## Notes

- The workflow engine does **NOT** implement Keycloak authentication logic
- All authentication/authorization is delegated to the Keycloak dependency
- The workflow engine only reads request attributes set by the Keycloak filter
- The Keycloak dependency is responsible for token validation and claim extraction
