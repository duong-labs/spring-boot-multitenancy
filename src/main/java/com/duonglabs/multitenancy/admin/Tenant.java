package com.duonglabs.multitenancy.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** A row of the tenant registry, stored in the main database. */
@Entity
public class Tenant {
    @Id
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String dbUrl;
    @Column(nullable = false)
    private String dbUser;
    private String dbPassword;

    protected Tenant() {
    }

    public Tenant(String code, String name, String dbUrl, String dbUser, String dbPassword) {
        this.code = code;
        this.name = name;
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDbUrl() { return dbUrl; }
    public String getDbUser() { return dbUser; }
    public String getDbPassword() { return dbPassword; }
}
