# Keycloak Claims for Workflow Engine Admins

## Overview

Workflow engine admins require special authorization to manage applications, view all tenants, and perform administrative operations. This document describes the Keycloak role that must be assigned for admin users.

> **Note:** For complete Keycloak dependency integration requirements, see [`KEYCLOAK_DEPENDENCY_CONTRACT.md`](./KEYCLOAK_DEPENDENCY_CONTRACT.md).

## Required Role

### Admin Role

**Role Name:** `WORKFLOW_ADMIN`

**Claim Name:** `roles` (or `realm_access.roles`)

**Type:** Array of Strings or String

**Description:** Users with this role are granted workflow engine admin privileges.

**Example:**
```json
{
  "roles": ["WORKFLOW_ADMIN", "user"]
}
```

or

```json
{
  "realm_access": {
    "roles": ["WORKFLOW_ADMIN", "user"]
  }
}
```

## Admin Capabilities

When a user has admin authorization, they can:

1. **Access Admin Endpoints:**
   - `GET /api/applications` - List all registered applications
   - `GET /api/applications/{appId}` - Get any application details
   - `PUT /api/applications/{appId}` - Update any application
   - `DELETE /api/applications/{appId}` - Deactivate any application

2. **Bypass Schema Requirements:**
   - Admin endpoints don't require a tenant schema
   - Admins can access admin operations without being tied to a specific tenant

3. **Schema Override (Optional):**
   - Admins can optionally set `X-Admin-Schema-Override` header to access a specific tenant's data
   - Useful for admin operations that need to work with a specific tenant's workflows

## Keycloak Configuration

### Step 1: Create WORKFLOW_ADMIN Role

1. In Keycloak Admin Console:
   - Go to Realm → Roles → Add Role
   - Create role: `WORKFLOW_ADMIN`
   - Description: "Workflow Engine Administrator"

### Step 2: Assign Role to Admin Users

1. Go to Users → Select Admin User → Role Mappings
2. Assign `WORKFLOW_ADMIN` role to the user

### Step 3: Configure Role Claim in Token

1. Go to Clients → Your Client → Mappers
2. Ensure "realm roles" mapper is configured:
   - **Name:** `realm roles`
   - **Mapper Type:** User Realm Role
   - **Token Claim Name:** `roles` (or `realm_access.roles`)
   - **Add to ID token:** ON
   - **Add to access token:** ON
   - **Multivalued:** ON

## Request Flow

### For Admin Users:

```
1. User logs in via Keycloak
2. Keycloak sets claims: roles=["WORKFLOW_ADMIN", ...]
3. fractalhive-spring-boot-starter-keycloak extracts claims and sets in request attribute
4. AdminAuthorizationFilter checks for WORKFLOW_ADMIN role
5. Sets isWorkflowAdmin=true in request attribute
6. TenantFilter allows admin endpoints without schema requirement
7. Admin can access /api/applications endpoints
```

### For Regular Users:

```
1. User logs in via Keycloak
2. Keycloak sets claims: schema="app_webapp1" (no workflow_admin claim)
3. External Keycloak dependency extracts claims and sets in request
4. AdminAuthorizationFilter: no admin claim found
5. TenantFilter requires schema name
6. User can only access tenant-specific endpoints
```

## Example JWT Token Claims

### Admin User Token:
```json
{
  "sub": "admin-user-id",
  "email": "admin@workflowengine.com",
  "schema": null,
  "roles": ["WORKFLOW_ADMIN", "user"]
}
```

### Regular Tenant User Token:
```json
{
  "sub": "user-id",
  "email": "user@webapp1.com",
  "schema": "app_webapp1",
  "roles": ["user"]
}
```

## Integration with Keycloak Dependency

The workflow engine integrates with `fractalhive-spring-boot-starter-keycloak` dependency.

### Required Dependency

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.fractalhive</groupId>
    <artifactId>fractalhive-spring-boot-starter-keycloak</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Keycloak Dependency Responsibilities

Your Keycloak dependency (`fractalhive-spring-boot-starter-keycloak`) should:

1. **Extract Claims:**
   - Read `roles` claim (array) from JWT token
   - Read `schema` claim (string) for tenant users

2. **Set Request Attributes:**
   - Set `roles` attribute (List<String>) from JWT token
   - Set `schemaName` attribute (String) for tenant users

3. **Implementation:**
   The `fractalhive-spring-boot-starter-keycloak` dependency should handle this automatically.
   The dependency extracts claims from JWT tokens and sets them as request attributes:
   
   - **Roles:** Extracted from `roles` claim (array) → Set as `request.setAttribute("roles", roles)`
   - **Schema:** Extracted from `schema` claim (string) → Set as `request.setAttribute("schemaName", schema)`
   
   If you need to customize the implementation, ensure these attributes are set before the workflow engine filters run.

## Admin Endpoints Summary

| Endpoint | Method | Admin Required | Schema Required |
|----------|--------|----------------|-----------------|
| `/api/applications/register` | POST | No | No |
| `/api/applications` | GET | Yes | No |
| `/api/applications/{appId}` | GET | Yes | No |
| `/api/applications/{appId}` | PUT | Yes | No |
| `/api/applications/{appId}` | DELETE | Yes | No |
| `/api/users` | GET | No | Yes |
| `/api/workflow-definitions/*` | * | No | Yes |
| `/api/tasks/*` | * | No | Yes |

## Testing Admin Access

### Test Admin Token:
```bash
# Login as admin and get token
TOKEN="your-admin-jwt-token"

# Access admin endpoint (no schema header needed)
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/applications
```

### Test Admin with Schema Override:
```bash
# Admin accessing specific tenant's data
curl -H "Authorization: Bearer $TOKEN" \
     -H "X-Admin-Schema-Override: app_webapp1" \
     http://localhost:8080/api/workflow-definitions
```

## Security Notes

1. **Admin Claims:** Only set `workflow_admin=true` for trusted admin users
2. **Role Validation:** Ensure `WORKFLOW_ADMIN` role is properly secured in Keycloak
3. **Schema Override:** Admin schema override should be validated against registered applications
4. **Audit Logging:** All admin operations should be logged with admin user ID

## Troubleshooting

### Admin Not Recognized

**Problem:** Admin user cannot access admin endpoints

**Solutions:**
1. Verify `WORKFLOW_ADMIN` role is assigned to the user in Keycloak
2. Verify `WORKFLOW_ADMIN` role is included in `roles` claim in JWT token
3. Check that external Keycloak dependency sets `roles` request attribute correctly
4. Verify `AdminAuthorizationFilter` runs before `TenantFilter` (Order 0 vs Order 1)
5. Check JWT token contains: `"roles": ["WORKFLOW_ADMIN", ...]`

### Schema Required for Admin

**Problem:** Admin user still requires schema header

**Solutions:**
1. Verify admin claim is being set correctly
2. Check that admin endpoint paths match `/api/applications/*` (except `/register`)
3. Verify filter order: `AdminAuthorizationFilter` (Order 0) before `TenantFilter` (Order 1)
