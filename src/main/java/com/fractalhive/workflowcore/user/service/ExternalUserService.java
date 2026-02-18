package com.fractalhive.workflowcore.user.service;

import com.fractalhive.workflowcore.application.entity.RegisteredApplication;
import com.fractalhive.workflowcore.application.repository.RegisteredApplicationRepository;
import com.fractalhive.workflowcore.common.dto.PaginatedResponse;
import com.fractalhive.workflowcore.user.dto.ExternalUser;
import com.fractalhive.workflowcore.user.dto.UserListRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service for fetching users from external application APIs.
 * Caches RegisteredApplication lookup per request to avoid N+1 queries.
 */
@Service
public class ExternalUserService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalUserService.class);

    private final RegisteredApplicationRepository applicationRepository;
    private final RestTemplate restTemplate;

    // Thread-local cache for application lookup (per request)
    private static final ThreadLocal<RegisteredApplication> applicationCache = new ThreadLocal<>();

    public ExternalUserService(
            RegisteredApplicationRepository applicationRepository,
            RestTemplate userApiRestTemplate) {
        this.applicationRepository = applicationRepository;
        this.restTemplate = userApiRestTemplate;
    }

    /**
     * Gets users from external API based on schema name.
     * Caches RegisteredApplication lookup to avoid repeated DB calls.
     *
     * @param schemaName the schema name (tenant identifier)
     * @param request    the user list request with filters
     * @return paginated response with users from external API
     * @throws IllegalArgumentException if schema not found or userApi endpoint missing
     */
    public PaginatedResponse<ExternalUser> getUsers(String schemaName, UserListRequest request) {
        RegisteredApplication application = getApplicationCached(schemaName);
        String userApiUrl = getApiEndpoint(application, "userApi");

        try {
            // Build URL with query parameters
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(userApiUrl + "/users");

            if (request.getSearch() != null && !request.getSearch().trim().isEmpty()) {
                builder.queryParam("search", request.getSearch());
            }
            if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
                builder.queryParam("role", request.getRole());
            }
            if (request.getLimit() != null && request.getLimit() > 0) {
                builder.queryParam("limit", request.getLimit());
            }

            String url = builder.toUriString();

            // Prepare headers with API key
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(application.getApiKey());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Call external API - expect response with "users" and "total" fields
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                return PaginatedResponse.<ExternalUser>builder()
                        .content(Collections.emptyList())
                        .page(0)
                        .size(0)
                        .totalElements(0)
                        .totalPages(0)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build();
            }

            // Extract users list and total from response
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> usersList = (List<Map<String, Object>>) responseBody.get("users");
            Integer total = responseBody.get("total") != null 
                    ? ((Number) responseBody.get("total")).intValue() 
                    : 0;

            List<ExternalUser> users = Collections.emptyList();
            if (usersList != null) {
                users = usersList.stream()
                        .map(this::mapToExternalUser)
                        .toList();
            }

            int limit = request.getLimit() != null && request.getLimit() > 0 ? request.getLimit() : 100;
            int size = users.size();
            long totalElements = total != null ? total : size;
            int totalPages = limit > 0 ? (int) Math.ceil((double) totalElements / limit) : 1;

            logger.debug("Fetched {} users from external API for schema: {} (total: {})", size, schemaName, totalElements);

            return PaginatedResponse.<ExternalUser>builder()
                    .content(users)
                    .page(0)
                    .size(size)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();

        } catch (RestClientException e) {
            logger.error("Error calling external user API for schema: {}", schemaName, e);
            throw new RuntimeException("Failed to fetch users from external API: " + e.getMessage(), e);
        }
    }

    /**
     * Maps a Map to ExternalUser DTO.
     *
     * @param userMap the user map from external API response
     * @return ExternalUser DTO
     */
    private ExternalUser mapToExternalUser(Map<String, Object> userMap) {
        return ExternalUser.builder()
                .userId((String) userMap.get("user_id"))
                .name((String) userMap.get("name"))
                .email((String) userMap.get("email"))
                .role((String) userMap.get("role"))
                .managerId((String) userMap.get("manager_id"))
                .build();
    }

    /**
     * Gets a single user by email from external API.
     *
     * @param schemaName the schema name
     * @param email      the user email
     * @return the user if found
     * @throws IllegalArgumentException if user not found
     */
    public ExternalUser getUserByEmail(String schemaName, String email) {
        PaginatedResponse<ExternalUser> response = getUsers(schemaName, UserListRequest.builder()
                .search(email)
                .limit(1)
                .build());

        return response.getContent().stream()
                .filter(user -> email.equals(user.getEmail()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    /**
     * Validates if a user exists in external API.
     *
     * @param schemaName the schema name
     * @param email      the user email
     * @return true if user exists, false otherwise
     */
    public boolean validateUserExists(String schemaName, String email) {
        try {
            getUserByEmail(schemaName, email);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Gets an API endpoint from application's apiEndpoints map.
     *
     * @param application the registered application
     * @param endpointKey the endpoint key (e.g., "userApi")
     * @return the endpoint URL
     * @throws IllegalArgumentException if endpoint not found
     */
    public String getApiEndpoint(RegisteredApplication application, String endpointKey) {
        Map<String, String> apiEndpoints = application.getApiEndpoints();
        if (apiEndpoints == null || !apiEndpoints.containsKey(endpointKey)) {
            throw new IllegalArgumentException("API endpoint not found: " + endpointKey);
        }
        return apiEndpoints.get(endpointKey);
    }

    /**
     * Gets an API endpoint by schema name and endpoint key.
     * Caches application lookup to avoid repeated DB calls.
     *
     * @param schemaName  the schema name
     * @param endpointKey the endpoint key
     * @return the endpoint URL
     */
    public String getApiEndpoint(String schemaName, String endpointKey) {
        RegisteredApplication application = getApplicationCached(schemaName);
        return getApiEndpoint(application, endpointKey);
    }

    /**
     * Gets application with caching per request.
     * Uses thread-local cache to avoid repeated DB queries.
     *
     * @param schemaName the schema name
     * @return the registered application
     */
    private RegisteredApplication getApplicationCached(String schemaName) {
        RegisteredApplication cached = applicationCache.get();
        if (cached != null && schemaName.equals(cached.getSchemaName())) {
            return cached;
        }

        RegisteredApplication application = applicationRepository.findBySchemaName(schemaName)
                .orElseThrow(() -> new IllegalArgumentException("Application not found for schema: " + schemaName));

        applicationCache.set(application);
        return application;
    }

    /**
     * Clears the application cache.
     * Should be called at the end of request processing.
     */
    public void clearCache() {
        applicationCache.remove();
    }
}
