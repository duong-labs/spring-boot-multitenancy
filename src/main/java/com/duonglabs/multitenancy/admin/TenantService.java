package com.duonglabs.multitenancy.admin;

import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;

import com.duonglabs.multitenancy.config.MultitenantDataSource;

/** Creates tenants at runtime: new database, schema, routing, registry row. */
@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final MultitenantDataSource tenantDataSource;

    public TenantService(TenantRepository tenantRepository, MultitenantDataSource tenantDataSource) {
        this.tenantRepository = tenantRepository;
        this.tenantDataSource = tenantDataSource;
    }

    public synchronized Tenant create(String code, String name) {
        if (!code.matches("[a-z0-9_]{1,32}")) { // the code ends up in a JDBC URL
            throw new IllegalArgumentException("code must match [a-z0-9_]{1,32}");
        }
        if (tenantRepository.existsById(code)) {
            throw new IllegalStateException("Tenant already exists: " + code);
        }

        String dbUrl = "jdbc:h2:mem:tenant_" + code + ";DB_CLOSE_DELAY=-1";
        DataSource dataSource = new DriverManagerDataSource(dbUrl, "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("tenant-schema.sql")).execute(dataSource);

        tenantDataSource.addTenant(code, dataSource);
        return tenantRepository.save(new Tenant(code, name, dbUrl));
    }
}
