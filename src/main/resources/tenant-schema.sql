-- Run against every new tenant database by TenantService.
CREATE TABLE check_in (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(255) NOT NULL,
    checked_in_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
