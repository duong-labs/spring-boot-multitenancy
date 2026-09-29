package com.duonglabs.multitenancy.config;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.duonglabs.multitenancy.admin.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Reads the tenant from the {@code X-Tenant-Code} header for the duration of the request. */
@Component
public class TenantFilter extends OncePerRequestFilter {

    private final MultitenantDataSource tenantDataSource;

    public TenantFilter(MultitenantDataSource tenantDataSource) {
        this.tenantDataSource = tenantDataSource;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/check-ins"); // /tenants only uses the main database
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String tenantCode = request.getHeader("X-Tenant-Code");
        if (tenantCode == null) {
            response.sendError(400, "Missing header X-Tenant-Code");
        } else if (!tenantDataSource.hasTenant(tenantCode)) {
            response.sendError(404, "Unknown tenant " + tenantCode);
        } else {
            TenantContext.set(tenantCode);
            try {
                chain.doFilter(request, response);
            } finally {
                TenantContext.clear(); // request threads are reused
            }
        }
    }
}
