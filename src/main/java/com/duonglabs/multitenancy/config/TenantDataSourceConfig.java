package com.duonglabs.multitenancy.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * The "tenant" side: a routing DataSource that picks one database per request, and the JPA
 * plumbing (package {@code domain}) built on top of it. These beans are {@code @Primary}, so
 * anything that does not name a main-side bean goes to the current tenant's database.
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.duonglabs.multitenancy.domain",
    entityManagerFactoryRef = "tenantEntityManagerFactory",
    transactionManagerRef = "tenantTransactionManager"
)
public class TenantDataSourceConfig {

    @Bean
    @Primary
    public MultitenantDataSource tenantDataSource() {
        MultitenantDataSource routing = new MultitenantDataSource();
        routing.setTargetDataSources(new HashMap<>()); // starts empty; tenants are added at runtime
        // No default target: an unknown tenant must fail, never fall back to another tenant's data.
        routing.afterPropertiesSet();
        return routing;
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean tenantEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("tenantDataSource") DataSource tenantDataSource) {
        // At boot there is no current tenant, so Hibernate must not open a connection to work out
        // the dialect: declare it and turn the JDBC metadata lookup off.
        Map<String, Object> jpaProperties = new HashMap<>();
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        jpaProperties.put("hibernate.temp.use_jdbc_metadata_defaults", false);

        return builder
                .dataSource(tenantDataSource)
                .packages("com.duonglabs.multitenancy.domain")
                .properties(jpaProperties)
                .build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager tenantTransactionManager(
            @Qualifier("tenantEntityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(Objects.requireNonNull(emf.getObject()));
    }

    /** One small connection pool per tenant database. */
    public static HikariDataSource buildTenantDataSource(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(0);
        return new HikariDataSource(config);
    }
}
