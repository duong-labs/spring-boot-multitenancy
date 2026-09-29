package com.duonglabs.multitenancy.admin;

import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;

import com.duonglabs.multitenancy.config.MultitenantDataSource;
import com.duonglabs.multitenancy.config.TenantDataSourceConfig;

/**
 * Creates a tenant at runtime, in code:
 * <ol>
 *   <li>create the tenant's own (in-memory) database and its tables,</li>
 *   <li>register its DataSource in the routing DataSource so requests can reach it,</li>
 *   <li>record it in the tenant registry (main database).</li>
 * </ol>
 */
@Service
public class TenantService {
    // The code ends up in a JDBC URL, so keep it strict.
    private static final Pattern CODE_PATTERN = Pattern.compile("[a-z0-9_]{1,32}");

    private final TenantRepository tenantRepository;
    private final MultitenantDataSource tenantDataSource;

    public TenantService(TenantRepository tenantRepository, MultitenantDataSource tenantDataSource) {
        this.tenantRepository = tenantRepository;
        this.tenantDataSource = tenantDataSource;
    }

    public synchronized Tenant create(String code, String name) {
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("code must match " + CODE_PATTERN.pattern());
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (tenantRepository.existsById(code)) {
            throw new IllegalStateException("Tenant already exists: " + code);
        }

        // DB_CLOSE_DELAY=-1 keeps the in-memory database alive after the last connection closes.
        Tenant tenant = new Tenant(code, name, "jdbc:h2:mem:tenant_" + code + ";DB_CLOSE_DELAY=-1", "sa", "");

        DataSource dataSource = TenantDataSourceConfig.buildTenantDataSource(
                tenant.getDbUrl(), tenant.getDbUser(), tenant.getDbPassword());
        new ResourceDatabasePopulator(new ClassPathResource("tenant-schema.sql")).execute(dataSource);

        tenantDataSource.addResolvedDataSource(code, dataSource);
        return tenantRepository.save(tenant);
    }
}
