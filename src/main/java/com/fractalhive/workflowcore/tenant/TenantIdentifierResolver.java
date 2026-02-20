package com.fractalhive.workflowcore.tenant;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver {

    // Must match KeycloakWorkflowFilter's SCHEMA_NAME_ATTRIBUTE
    private static final String SCHEMA_NAME_ATTRIBUTE = "schemaName";
    public static final String DEFAULT_SCHEMA = "workflow_master";

    @Override
    public String resolveCurrentTenantIdentifier() {
        try {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                Object schema = requestAttributes.getAttribute(
                    SCHEMA_NAME_ATTRIBUTE,
                    RequestAttributes.SCOPE_REQUEST
                );
                if (schema != null && !schema.toString().isBlank()) {
                    return schema.toString();
                }
            }
        } catch (Exception e) {
            log.warn("Could not resolve tenant identifier from request context, using default", e);
        }

        return DEFAULT_SCHEMA;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}