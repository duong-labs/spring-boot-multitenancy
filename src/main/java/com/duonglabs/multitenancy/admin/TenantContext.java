package com.duonglabs.multitenancy.admin;

/** Holds the tenant code of the request being handled by the current thread. */
public final class TenantContext {
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void setCurrentTenant(String tenantCode) {
        CURRENT_TENANT.set(tenantCode);
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
