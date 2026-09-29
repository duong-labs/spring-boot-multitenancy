package com.duonglabs.multitenancy.config;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/** Tenant databases: a routing DataSource plus the JPA beans on top of it. {@code @Primary}, so they are the default. */
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
        return new MultitenantDataSource();
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean tenantEntityManagerFactory(
            EntityManagerFactoryBuilder builder, @Qualifier("tenantDataSource") DataSource dataSource) {
        // No tenant exists at boot: declare the dialect so Hibernate does not open a connection to detect it.
        return builder.dataSource(dataSource)
                .packages("com.duonglabs.multitenancy.domain")
                .properties(Map.of(
                        "hibernate.dialect", "org.hibernate.dialect.H2Dialect",
                        "hibernate.boot.allow_jdbc_metadata_access", false))
                .build();
    }

    @Bean
    @Primary
    public JpaTransactionManager tenantTransactionManager(
            @Qualifier("tenantEntityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }
}
