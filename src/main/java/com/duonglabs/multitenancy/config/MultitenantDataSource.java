package com.duonglabs.multitenancy.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.duonglabs.multitenancy.admin.TenantContext;

/** Routes each connection request to the DataSource of the current tenant. Tenants can be added at runtime. */
public class MultitenantDataSource extends AbstractRoutingDataSource {

    // The superclass keeps its map in a private non-volatile field; this copy is safe to read across threads.
    private volatile Map<Object, DataSource> routable = Map.of();

    public MultitenantDataSource() {
        setTargetDataSources(new HashMap<>());
        afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.get();
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        routable = super.getResolvedDataSources();
    }

    public synchronized void addTenant(String tenantCode, DataSource dataSource) {
        Map<Object, Object> targets = new HashMap<>(routable);
        targets.put(tenantCode, dataSource);
        setTargetDataSources(targets);
        afterPropertiesSet();
    }

    public boolean hasTenant(String tenantCode) {
        return routable.containsKey(tenantCode);
    }

    @Override
    protected DataSource determineTargetDataSource() {
        DataSource dataSource = routable.get(determineCurrentLookupKey());
        if (dataSource == null) { // never fall back to another tenant's database
            throw new IllegalStateException("No DataSource for tenant " + determineCurrentLookupKey());
        }
        return dataSource;
    }
}
