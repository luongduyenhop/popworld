# Project: PopWorld Security Hardening, Database Optimization, and Concurrency Load Testing

## Architecture
PopWorld is a Spring Boot 3 / Java 25 e-commerce and blind box (POP NOW) platform using Spring MVC, Thymeleaf, Spring Data JPA / Hibernate, MySQL (InnoDB), Spring Security, and Maven.

Key Architectural Layers:
- **Web / API Layer**: Controllers in `com.manguonmo.popworld.controller.client` and `admin`, plus REST API controllers (`api.*`). Filter chain configured in `SecurityConfig.java` and `RateLimitingFilter.java`.
- **Service Layer**: Transactional business services in `com.manguonmo.popworld.service.impl` implementing order processing, cart management, blind box unboxing, coupons, catalog, and inventory.
- **Data Access Layer**: Spring Data JPA repositories in `com.manguonmo.popworld.repository` managing entity state, JPQL queries, `@EntityGraph`, and locking (`@Lock(PESSIMISTIC_WRITE)`).
- **Domain Entity Layer**: JPA entities in `com.manguonmo.popworld.model` mapped to relational tables in MySQL.

## Feature Inventory
Every requirement and surveyed item is mapped below:
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Test Mock Baseline | Add `@MockitoBean WishlistService` to `SecurityConfigTest.java` to fix 14 failing tests and restore clean baseline. | M1 | Survey 1, 3 |
| 2 | Method Security & RBAC | Enable `@EnableMethodSecurity` in `SecurityConfig.java`, enforce `@PreAuthorize("hasRole('ADMIN')")` on all 11 admin controllers, fix `GlobalExceptionHandler` for 403. | M1 | Survey 1 (SEC-03, SEC-06) |
| 3 | IDOR Elimination | Replace `BadRequestException` (400) with `AccessDeniedException` (403) on order, cart, address, wishlist, and popnow queries when not owner/admin. Add `/wishlist/**` to `.authenticated()`. | M1 | Survey 1 (SEC-05, SEC-07) |
| 4 | CSRF Hardening | Remove CSRF exemption for `/api/popnow/simulate-payment`. Enforce HTTP POST only on `/popnow/checkout/{reservationCode}` with CSRF token verification. | M1 | Survey 1 (SEC-01, SEC-02) |
| 5 | Rate Limiting Defense | Add rules in `RateLimitingFilter` for `POST /login` (10/min), `POST /checkout/place-order` (15/min), `POST /order-tracking` (15/min), and `/api/popnow/reserve` (20/min). | M1 | Survey 1 (SEC-04) |
| 6 | Schema Database Indexing | Add composite and single `@Index` definitions in JPA entities for `products`, `orders`, `order_items`, `reviews`, `user_addresses`, and `product_images`. | M2 | Survey 2 (Obs 5) |
| 7 | HikariCP & Hibernate Tuning | Configure HikariCP pool (`maximum-pool-size=30`, `minimum-idle=10`, timeouts) and `hibernate.default_batch_fetch_size=30` in `application.properties`. | M2 | Survey 2 (Obs 6) |
| 8 | N+1 Elimination (Catalog & Cart) | Optimize catalog queries in `ProductRepository` & `ProductWebController` using `@EntityGraph` / `JOIN FETCH` / SQL filtering. Optimize cart mapping and navbar count. | M2 | Survey 2 (Obs 1, Obs 2) |
| 9 | N+1 Elimination (Orders, PopNow, Reviews) | Batch load order items using `findByOrderIdIn` in `OrderServiceImpl` and `AccountWebController`. Optimize `PopNowServiceImpl` cabinet and `ReviewRepository` queries. | M2 | Survey 2 (Obs 3, Obs 4) |
| 10 | Transaction Optimization | Add `@Transactional(readOnly = true)` to read methods across `CategoryServiceImpl`, `CharacterIpServiceImpl`, `OrderServiceImpl`, `ProductServiceImpl`, `CartServiceImpl`. | M2 | Survey 2 (Obs 7) |
| 11 | Stock Concurrency Safety | Add `@Modifying(clearAutomatically = true)` to `ProductRepository.updateStock`. Implement sorted product locking in `OrderServiceImpl.createOrder` to prevent deadlocks. | M3 | Survey 3 (Obs 1) |
| 12 | POP NOW Unboxing Concurrency Safety | Harden `PopNowServiceImpl.getOrCreateSlotForUpdate` against unique constraint rollback; guarantee serialized atomic unboxing transitions to `UNBOXED`. | M3 | Survey 3 (Obs 2) |
| 13 | Coupon Usage Limit Concurrency Safety | Implement atomic lock / pessimistic check on `UserCoupon` in `CouponServiceImpl` to prevent concurrent double-redemption by the same user. | M3 | Survey 3 (Obs 3) |
| 14 | Concurrency Integration Test Suite | Multi-threaded test cases using `ExecutorService`, `CountDownLatch`, `CompletableFuture` for Stock race condition, POP NOW unboxing, and Coupon limit. | M4 | Survey 3 (Obs 4, Blueprint) |
| 15 | Load & Performance Benchmark | Measure and benchmark RPS, p95 latency, and error rates under concurrent load, confirming zero data corruption or negative stock. | M4 | Survey 3 (Obs 4, Blueprint) |
| 16 | Final Verification & Master Report | 100% test pass rate (`mvn test`), produce comprehensive `AUDIT_LOADTEST_SECURITY_REPORT.md` documenting security, performance, concurrency, and production guidance. | M5 | Acceptance Criteria |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Security Hardening | Features 1, 2, 3, 4, 5 (Test mock fix, Method security, IDOR 403, CSRF enforcement, Rate limiting) | none | DONE (150 tests pass, Clean Audit) |
| M2 | Database & JPA Optimization | Features 6, 7, 8, 9, 10 (Indexes, HikariCP, Hibernate batch size, N+1 query elimination, @Transactional) | M1 | IN_PROGRESS |
| M3 | Concurrency Defenses & Locking | Features 11, 12, 13 (Stock update cache clear & sorted lock, POP NOW atomic unbox, UserCoupon concurrency) | M2 | PLANNED |
| M4 | Concurrency Integration & Load Testing | Features 14, 15 (Multi-threaded integration tests, load metrics RPS/p95, invariant assertions) | M3 | PLANNED |
| M5 | Regression Verification & Deliverable | Feature 16 (Full build/test verification, AUDIT_LOADTEST_SECURITY_REPORT.md) | M4, E2E | PLANNED |

## Interface Contracts
### Security ↔ Controllers & Services
- All IDOR violations must throw `org.springframework.security.access.AccessDeniedException` or return HTTP 403 / 404.
- `GlobalExceptionHandler` must handle `AccessDeniedException` and return HTTP 403 with standard `ApiResponse(success=false, message=...)`.
- Method security enabled globally via `@EnableMethodSecurity`.

### Database & Repository Contracts
- Entity indexes generated via JPA `@Table(indexes = { @Index(...) })`.
- Fetching collections/associations on catalog and cart must use `@EntityGraph` or `JOIN FETCH` without Cartesian product duplication.
- Repositories modifying database records in JPA transactions must specify `@Modifying(clearAutomatically = true)`.

### Concurrency & Locking Contracts
- Product stock deduction: atomic SQL update with `stockQuantity >= :quantity` and L1 cache eviction. Multi-product cart checkout must sort product IDs before locking/deducting.
- POP NOW blind box slot: Pessimistic write lock (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) on reservation and slot. Status transition to `UNBOXED` must be strictly idempotent and atomic.
- User coupon validation: Pessimistic lock or atomic update condition ensuring single use per user.

## Code Layout
- Configuration: `src/main/java/com/manguonmo/popworld/config/`
- Security Filters: `src/main/java/com/manguonmo/popworld/security/`
- Controllers: `src/main/java/com/manguonmo/popworld/controller/`
- Service Interfaces & Implementations: `src/main/java/com/manguonmo/popworld/service/`
- Repositories: `src/main/java/com/manguonmo/popworld/repository/`
- Entities: `src/main/java/com/manguonmo/popworld/model/`
- Mappers & DTOs: `src/main/java/com/manguonmo/popworld/mapper/`, `dto/`
- Test Suite: `src/test/java/com/manguonmo/popworld/`
- Deliverable: `d:/MaNguonMo/popworld/AUDIT_LOADTEST_SECURITY_REPORT.md`
