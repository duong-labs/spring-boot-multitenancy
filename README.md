# Multitenancy với Spring Boot — database-per-tenant

Demo tối giản về **multitenancy kiểu "mỗi tenant một database"** bằng Spring Boot + Spring Data JPA.
Ý tưởng được rút gọn từ một service thật (check-in-service): chỉ giữ lại phần cấu hình
*multiple datasources / multiple entity managers*, còn lại (security, JWT, Liquibase, scheduler...) đã bỏ.

- Java 21, Spring Boot 3.5, H2 **in-memory** (không cần cài DB, tắt app là mất dữ liệu).
- Không Liquibase: database và bảng của tenant mới được tạo **bằng code** lúc runtime.

## Chạy thử

```bash
./gradlew bootRun          # http://localhost:8080
./gradlew test             # test chứng minh dữ liệu các tenant không lẫn vào nhau
```

```bash
H='Content-Type: application/json'

# 1. Tạo 2 tenant lúc app đang chạy
curl -XPOST localhost:8080/tenants -H "$H" -d '{"code":"acme","name":"Acme"}'
curl -XPOST localhost:8080/tenants -H "$H" -d '{"code":"globex","name":"Globex"}'

# 2. Check-in cho acme (tenant được chọn bằng header X-Tenant-Code)
curl -XPOST localhost:8080/check-ins -H "$H" -H 'X-Tenant-Code: acme' -d '{"customerName":"alice"}'

# 3. Mỗi tenant chỉ thấy data của mình
curl localhost:8080/check-ins -H 'X-Tenant-Code: acme'     # -> [alice]
curl localhost:8080/check-ins -H 'X-Tenant-Code: globex'   # -> []
curl localhost:8080/tenants                                 # danh sách tenant (main DB)
```

## Ý tưởng

Có 3 kiểu multitenancy phổ biến:

| Kiểu | Cách cô lập | Ưu | Nhược |
|---|---|---|---|
| Cột `tenant_id` | chung DB, chung bảng | rẻ, đơn giản | quên `WHERE tenant_id` là lộ data |
| Schema-per-tenant | chung DB, khác schema | vừa phải | migration nhân lên theo số schema |
| **Database-per-tenant** (demo này) | mỗi tenant một DB riêng | cô lập mạnh, backup/restore/xoá từng tenant dễ | tốn connection, phải quản lý nhiều DB |

Với database-per-tenant, code nghiệp vụ **không cần biết tenant là ai**: `CheckInRepository.findAll()`
không có `WHERE tenant_id`, vì nó chỉ đang nói chuyện với DB của tenant hiện tại.
Điều cần làm là *chọn đúng DB cho mỗi request*. Việc đó gồm 4 mảnh ghép:

```
 Request ──► TenantFilter ──► TenantContext (ThreadLocal) ──► MultitenantDataSource ──► DB của tenant
             đọc header        lưu "acme" cho thread          getConnection() tra
             X-Tenant-Code     này trong suốt request         "acme" → DataSource
```

### 1. Hai "thế giới" dữ liệu

| | Main DB | Tenant DB (mỗi tenant một cái) |
|---|---|---|
| Chứa | danh sách tenant (`Tenant`) | dữ liệu nghiệp vụ (`CheckIn`) |
| DataSource | 1 `DataSource` cố định | `MultitenantDataSource` (routing) |
| Package | `admin` | `domain` |
| Cấu hình | `MainDataSourceConfig` | `TenantDataSourceConfig` |

Vì có hai DataSource nên phải có **hai bộ** `EntityManagerFactory` + `TransactionManager`, và mỗi bộ được
gắn vào đúng package repository bằng `@EnableJpaRepositories(basePackages, entityManagerFactoryRef, transactionManagerRef)`.
Bộ của tenant được đánh dấu `@Primary`: bean nào không chỉ định rõ thì mặc định đi vào DB của tenant hiện tại.

### 2. `TenantContext` + `TenantFilter`
`TenantFilter` đọc header `X-Tenant-Code` và đặt vào `TenantContext` (một `ThreadLocal`), rồi **luôn xoá**
trong `finally` vì thread của Tomcat được tái sử dụng. (Service thật lấy tenant từ JWT thay vì header —
chỉ khác chỗ lấy tenant, cơ chế phía sau y hệt.)

### 3. `MultitenantDataSource` — trái tim của mô hình
Kế thừa `AbstractRoutingDataSource` của Spring: mỗi lần cần connection, nó gọi
`determineCurrentLookupKey()` (= `TenantContext.getCurrentTenant()`) rồi trả connection từ pool của tenant đó.
Hai điểm đáng chú ý:

- **Không có default target.** Tenant không tồn tại → lỗi, tuyệt đối không "rơi" sang DB của tenant khác.
- **Thêm tenant lúc runtime** (`addResolvedDataSource`, `synchronized`). Map nội bộ của
  `AbstractRoutingDataSource` không `volatile`, nên class giữ thêm một bản `volatile` để các thread request
  luôn thấy tenant vừa được thêm.

### 4. Tạo tenant mới bằng code — `TenantService.create`
1. Validate `code` (nó nằm trong JDBC URL nên phải chặt).
2. Tạo DataSource (Hikari pool) tới DB mới của tenant — với H2 in-memory, DB được tạo ngay khi có kết nối đầu tiên.
   Với MySQL/Postgres thật, đây là chỗ chạy `CREATE DATABASE`.
3. Chạy `tenant-schema.sql` để tạo bảng (thay cho Liquibase; service thật chạy Liquibase ở bước này).
4. Đăng ký DataSource vào `MultitenantDataSource` → từ giờ request với tenant này đi được.
5. Lưu một dòng vào bảng `tenant` ở main DB (registry).

## Vài chuyện dễ vấp (đã xử lý sẵn trong code)

- **Boot không được "thăm dò" routing DataSource.** Lúc khởi động chưa có request nên chưa có tenant; nếu Hibernate/Boot
  mở connection để đoán dialect hay DDL mode sẽ lỗi. Vì vậy: `spring.jpa.hibernate.ddl-auto=none`, khai báo dialect
  và `hibernate.temp.use_jdbc_metadata_defaults=false` cho tenant EMF. Main EMF tự đặt `hbm2ddl.auto=create` riêng.
- **Transaction lấy connection lúc bắt đầu.** Tenant phải được set *trước* khi vào `@Transactional` (filter chạy
  trước controller nên ổn). Đừng đổi tenant giữa chừng một transaction.
- **Thread khác không có tenant.** `ThreadLocal` không đi theo `@Async`/executor; nếu chạy job nền cho tenant nào,
  phải tự `setCurrentTenant` rồi `clear` trong `finally`.
- **`open-in-view` tắt** để EntityManager không bị mở sớm hơn lúc tenant được xác định.

## Khác gì so với service thật (những thứ cố ý bỏ)

| Service thật | Demo |
|---|---|
| MySQL, `CREATE DATABASE` | H2 in-memory |
| Liquibase migrate schema tenant | `tenant-schema.sql` chạy bằng code |
| Tenant lấy từ JWT | header `X-Tenant-Code` |
| Boot đọc bảng `tenant` để dựng lại các pool; scheduled task đồng bộ tenant giữa nhiều instance | không cần — DB in-memory nên mất khi tắt app |
| Chia connection budget cho các pool, housekeeping executor dùng chung | mỗi pool 5 connection |
| Security, audit, quota, Quartz... | không có |

## Cấu trúc

```
src/main/java/com/duonglabs/multitenancy
├── config/   MainDataSourceConfig, TenantDataSourceConfig, MultitenantDataSource, TenantFilter
├── admin/    (main DB)   Tenant, TenantRepository, TenantService, TenantController, TenantContext
└── domain/   (tenant DB) CheckIn, CheckInRepository, CheckInController
src/main/resources/tenant-schema.sql   schema cho mỗi tenant mới
```
