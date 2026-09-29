package com.duonglabs.multitenancy.admin;

/** The tenant of the request running on the current thread. */
public final class TenantContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void set(String tenantCode) {
        CURRENT.set(tenantCode);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
