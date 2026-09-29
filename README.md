# Database-per-tenant multitenancy with Spring Boot

A minimal demo: every tenant gets its own database, and each request is routed to the right one.
Spring Boot 4.1.1, Java 27, in-memory H2 (nothing to install; data is lost on restart).

```bash
./gradlew bootRun     # http://localhost:8080
./gradlew test
```

```bash
H='Content-Type: application/json'
curl -XPOST localhost:8080/tenants -H "$H" -d '{"code":"acme","name":"Acme"}'
curl -XPOST localhost:8080/tenants -H "$H" -d '{"code":"globex","name":"Globex"}'

curl -XPOST localhost:8080/check-ins -H "$H" -H 'X-Tenant-Code: acme' -d '{"customerName":"alice"}'
curl localhost:8080/check-ins -H 'X-Tenant-Code: acme'     # [alice]
curl localhost:8080/check-ins -H 'X-Tenant-Code: globex'   # []
```

## How it works

```
request -> TenantFilter -> TenantContext (ThreadLocal) -> MultitenantDataSource -> tenant's database
           X-Tenant-Code    "acme" for this thread         picks the DataSource
```

- **Two kinds of database.** The *main* database (package `admin`) holds the tenant registry. Each
  *tenant* database (package `domain`) holds business data, with no `tenant_id` column: repositories
  just query whichever database the request was routed to.
- **Two sets of JPA beans.** Two DataSources mean two `EntityManagerFactory` + `TransactionManager` pairs
  (`MainDataSourceConfig`, `TenantDataSourceConfig`), each bound to its repository package via
  `@EnableJpaRepositories`. The tenant set is `@Primary`.
- **Routing.** `MultitenantDataSource` extends Spring's `AbstractRoutingDataSource` and uses
  `TenantContext` as the lookup key. There is no default target: an unknown tenant fails instead of
  falling back to someone else's data. Tenants can be added while the app runs.
- **Creating a tenant in code.** `TenantService.create` creates the database, runs `tenant-schema.sql`
  (no Liquibase), registers the DataSource for routing and saves a registry row.

## Pitfalls

- There is no tenant at boot, so Hibernate must not open a connection to the routing DataSource:
  `ddl-auto: none`, and the dialect is declared explicitly for the tenant `EntityManagerFactory`.
- A transaction picks its connection when it begins, so the tenant must be set before `@Transactional`
  code runs (the filter guarantees it) and never changed mid-transaction.
- `TenantContext` is a `ThreadLocal`: clear it in `finally`, and set it manually in background threads.

## Simplified for the demo

Tenant pools are plain unpooled DataSources, the tenant comes from a header instead of a JWT, and the
registry is not reloaded at startup (H2 is in-memory). A production setup would use HikariCP pools
sized per tenant, real database creation, a migration tool such as Liquibase, and reload the registry
at boot and on a schedule so every instance learns about new tenants.
