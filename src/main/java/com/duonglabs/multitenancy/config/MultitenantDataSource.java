package com.duonglabs.multitenancy.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.duonglabs.multitenancy.admin.TenantContext;

/**
 * Routes every {@code getConnection()} call to the DataSource of the tenant in {@link TenantContext}.
 * Tenants can be added while the application is running.
 */
public class MultitenantDataSource extends AbstractRoutingDataSource {

    // AbstractRoutingDataSource keeps its resolved map in a private, non-volatile field. Since
    // tenants are added at runtime from other threads, we publish our own volatile copy and read
    // that on the hot path.
    private volatile Map<Object, DataSource> routable = Map.of();

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getCurrentTenant();
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        routable = super.getResolvedDataSources();
    }

    public synchronized void addResolvedDataSource(String tenantCode, DataSource dataSource) {
        Map<Object, Object> targets = new HashMap<>(getResolvedDataSources());
        targets.put(tenantCode, dataSource);
        setTargetDataSources(targets);
        afterPropertiesSet();
    }

    public boolean hasTenant(String tenantCode) {
        return routable.containsKey(tenantCode);
    }

    @Override
    public synchronized Map<Object, DataSource> getResolvedDataSources() {
        return super.getResolvedDataSources();
    }

    @Override
    protected DataSource determineTargetDataSource() {
        Object key = determineCurrentLookupKey();
        DataSource dataSource = routable.get(key);
        if (dataSource == null) {
            throw new IllegalStateException("Cannot determine target DataSource for tenant [" + key + "]");
        }
        return dataSource;
    }
}
