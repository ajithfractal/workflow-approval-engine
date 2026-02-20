package com.fractalhive.workflowcore.tenant;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                String schemaName = jwt.getClaimAsString("schemaName");

                if (schemaName == null || schemaName.isBlank()) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing tenant schema");
                    return;
                }

                TenantContext.setCurrentTenant(schemaName);
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}