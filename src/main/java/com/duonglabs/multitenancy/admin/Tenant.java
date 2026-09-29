package com.duonglabs.multitenancy.admin;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** A tenant registry row, stored in the main database. */
@Entity
public class Tenant {
    @Id
    private String code;
    private String name;
    private String dbUrl;

    protected Tenant() {
    }

    public Tenant(String code, String name, String dbUrl) {
        this.code = code;
        this.name = name;
        this.dbUrl = dbUrl;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDbUrl() { return dbUrl; }
}
