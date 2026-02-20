package com.fractalhive.workflowcore.user.controller;

import com.fractalhive.workflowcore.common.dto.PaginatedResponse;
import com.fractalhive.workflowcore.tenant.SchemaRoutingDataSource;
import com.fractalhive.workflowcore.tenant.TenantIdentifierResolver;
import com.fractalhive.workflowcore.user.dto.ExternalUser;
import com.fractalhive.workflowcore.user.dto.UserListRequest;
import com.fractalhive.workflowcore.user.service.ExternalUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user management.
 * Fetches users from external application APIs based on current tenant schema.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@Tag(name = "Users", description = "APIs for fetching users from external application APIs")
public class UserController {

    private final ExternalUserService userService;
    private final TenantIdentifierResolver tenantIdentifierResolver;

    /**
     * Lists users from external API.
     * Uses schema from request context (set by TenantFilter).
     *
     * @param search optional search query
     * @param role   optional role filter
     * @param limit  optional limit (default: 100)
     * @return paginated response with users
     */
    @GetMapping
    @Operation(
            summary = "List users",
            description = "Fetches users from external application API based on current tenant schema"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved users"),
            @ApiResponse(responseCode = "403", description = "Schema not found or invalid"),
            @ApiResponse(responseCode = "500", description = "Error calling external API")
    })
    public ResponseEntity<PaginatedResponse<ExternalUser>> listUsers(
            @Parameter(description = "Search query (name or email)")
            @RequestParam(required = false) String search,
            @Parameter(description = "Role filter")
            @RequestParam(required = false) String role,
            @Parameter(description = "Maximum number of results")
            @RequestParam(required = false) Integer limit) {

        // Get schema from request context
        String schemaName = tenantIdentifierResolver.resolveCurrentTenantIdentifier();
        if (schemaName == null || "workflow_master".equals(schemaName)) {
            return ResponseEntity.status(403).build();
        }

        UserListRequest request = UserListRequest.builder()
                .search(search)
                .role(role)
                .limit(limit)
                .build();

        PaginatedResponse<ExternalUser> response = userService.getUsers(schemaName, request);
        return ResponseEntity.ok(response);
    }
}
