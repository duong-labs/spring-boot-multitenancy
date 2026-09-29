package com.duonglabs.multitenancy.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** Business data, stored in each tenant's own database (no tenant_id column). */
@Entity
public class CheckIn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String customerName;
    @Column(nullable = false)
    private Instant checkedInAt;

    protected CheckIn() {
    }

    public CheckIn(String customerName) {
        this.customerName = customerName;
        this.checkedInAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getCustomerName() { return customerName; }
    public Instant getCheckedInAt() { return checkedInAt; }
}
