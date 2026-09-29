package com.duonglabs.multitenancy.config;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.duonglabs.multitenancy.admin.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Resolves the tenant of a request from the {@code X-Tenant-Code} header and stores it in
 * {@link TenantContext} for the duration of the request. (check-in-service takes it from the JWT
 * audience instead; a header keeps this demo free of authentication.)
 */
@Component
public class TenantFilter extends OncePerRequestFilter {
    static final String HEADER = "X-Tenant-Code";

    private final MultitenantDataSource tenantDataSource;

    public TenantFilter(MultitenantDataSource tenantDataSource) {
        this.tenantDataSource = tenantDataSource;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // /tenants talks to the main database, no tenant needed.
        return !request.getRequestURI().startsWith("/check-ins");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String tenantCode = request.getHeader(HEADER);
        if (!StringUtils.hasText(tenantCode)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing header " + HEADER);
            return;
        }
        if (!tenantDataSource.hasTenant(tenantCode)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unknown tenant " + tenantCode);
            return;
        }
        try {
            TenantContext.setCurrentTenant(tenantCode);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear(); // threads are pooled: never leak a tenant into the next request
        }
    }
}
