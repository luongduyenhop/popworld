# PopWorld E2E Test Infrastructure Specification (TEST_INFRA.md)

## 1. Overview & Objectives

This document establishes the architecture, test harness conventions, data isolation strategies, and execution methodology for the **PopWorld End-to-End (E2E) Test Suite**.

The test suite provides exhaustive, requirement-driven verification across all layers of the PopWorld platform (Web, REST API, Service, Security, and Persistence), adhering strictly to the **4-Tier Testing Methodology**:
- **Tier 1: Feature Coverage** — Core functional happy paths and fundamental security barriers (RBAC, IDOR, CSRF, rate limits, stock updates, blind box slots, coupon calculation).
- **Tier 2: Boundary & Corner Cases** — Extreme values, zero/negative stock, expired/exhausted coupons, invalid state transitions, and exception boundaries.
- **Tier 3: Cross-Feature Combinations** — Inter-feature workflows (coupon redemption coupled with low inventory, blind box reservation to order conversion, multi-item cart partial selection).
- **Tier 4: Real-World Application Scenarios** — Full customer journeys (from product discovery, cart, coupon, checkout, VietQR payment webhook, to order fulfillment; complete blind box unbox-to-cabinet-to-delivery lifecycle).

---

## 2. Environment & Technical Stack

| Component | Specification |
|---|---|
| **Language Runtime** | Java 25 (Eclipse Temurin JDK 25.0.3) |
| **Framework** | Spring Boot 4.1.1 (Spring Framework 6.x) |
| **Security Layer** | Spring Security 6 + Custom Rate Limiter Filter |
| **Persistence Layer** | Spring Data JPA / Hibernate 7.4.5 |
| **Database Engine** | MySQL 8.0 (InnoDB) running in Docker (`popworld-mysql` container on port 3306) |
| **Test Engine** | JUnit 5 (Jupiter), AssertJ, Spring Test (`@SpringBootTest`, `@AutoConfigureMockMvc`, MockMvc) |
| **Build Tool** | Apache Maven Wrapper (`mvnw.cmd`) |

---

## 3. Four-Tier Test Suite Architecture

The test suite is structured under `src/test/java/com/manguonmo/popworld/e2e/`:

```
src/test/java/com/manguonmo/popworld/e2e/
├── base/
│   └── BaseE2ETest.java                     # Common test fixtures, test data builders, and auth helpers
├── tier1/
│   ├── Tier1SecurityEndpointsE2ETest.java   # RBAC, IDOR, CSRF, and Rate Limiting defenses
│   └── Tier1FeatureCoverageE2ETest.java     # Stock deduction, blind box reservation, coupon engine
├── tier2/
│   ├── Tier2BoundaryCornerCasesE2ETest.java # Out-of-stock, negative quantity, expired/depleted coupons
│   └── Tier2StateTransitionsE2ETest.java    # Strict order and reservation state machine transitions
├── tier3/
│   └── Tier3CrossFeatureCombinationsE2ETest.java # Coupon + low stock, reservation checkout, multi-item cart
└── tier4/
    └── Tier4RealWorldScenariosE2ETest.java   # Full end-to-end shopping & POP NOW unbox-to-delivery journeys
```

---

## 4. Detailed Tier Specifications & Requirement Mapping

### Tier 1: Feature Coverage
- **Security Endpoints & RBAC**:
  - Unauthenticated access to `/admin/**` redirects to `/login` (302).
  - Authenticated `ROLE_USER` attempting to access `/admin/**` receives 403 Forbidden.
  - Authenticated `ROLE_ADMIN` successfully accesses `/admin/**`.
- **IDOR Rejection**:
  - Attempting to view another user's order via `/api/orders/{orderCode}` is rejected (403/400).
  - Attempting to manipulate another user's cart item (`/api/cart/items/{id}`) is rejected.
  - Attempting to cancel or unbox another user's reservation is rejected.
- **CSRF Protection**:
  - State-altering requests (POST/PUT/DELETE) without valid CSRF token fail with 403 Forbidden.
  - Authorized requests with valid CSRF token proceed normally.
- **Query & Rate Limiting Thresholds**:
  - Exceeding request limits on sensitive operations (coupon validate, register, cart mutation) triggers HTTP 429 Too Many Requests.
- **Core Business Logic**:
  - Stock deduction updates inventory cleanly upon ordering.
  - Blind box slot reservation transitions slot to `HELD` and reservation to `RESERVED` with a 5-minute TTL.
  - Coupon calculation accurately handles PERCENT discounts (with cap) and FIXED discounts.

### Tier 2: Boundary & Corner Cases
- **Stock Depletion & Bounds**:
  - Attempting to purchase item with `stockQuantity = 0` triggers `OutOfStockException` / rejection.
  - Adding zero or negative quantities (`quantity <= 0`) is rejected.
  - Requesting more units than currently in stock fails safely without inventory corruption.
- **Coupons Boundaries**:
  - Expired coupons (`endDate < today`) are rejected.
  - Coupons with `usedCount >= usageLimit` are rejected.
  - Inactive coupons (`active = false`) are rejected.
  - Orders below `minOrderAmount` cannot redeem the coupon.
- **State Machine Violations**:
  - Cannot cancel an already `CANCELLED` or `EXPIRED` order.
  - Customer cannot cancel orders in `PROCESSING`, `PACKED`, `SHIPPING`, or `DELIVERED` status.
  - Cannot pack an order unless status is `PROCESSING`.
  - Cannot ship an order unless status is `PACKED` or `PROCESSING`.
  - Cannot complete an order unless status is `SHIPPING`.
  - Cannot unbox a reservation in `RESERVED`, `CANCELLED`, or `EXPIRED` status.

### Tier 3: Cross-Feature Combinations
- **Coupon + Exact/Low Stock Checkout**:
  - Placing an order for the exact remaining stock units with an active coupon succeeds, reducing stock to 0 and incrementing coupon usage.
  - Cancelling the order restores stock to original level and releases the coupon for re-use.
- **Blind Box Reservation + Checkout**:
  - Reserving a blind box locks the slot and decrements stock.
  - Generating an order for the reservation transitions status correctly without double-decrementing stock.
  - Simulating payment marks reservation as `PURCHASED` and slot as `SOLD`.
- **Multi-Item Cart with Dynamic Shipping**:
  - Adding multiple products to cart.
  - Unchecking select items: only checked items are converted to order lines.
  - Subtotal threshold logic: Free shipping (0 VND) when subtotal >= 500,000 VND; 30,000 VND shipping fee when subtotal < 500,000 VND.

### Tier 4: Real-World Application Scenarios
- **Scenario 1 — Complete E2E Customer Shopping Journey**:
  1. Authenticate user.
  2. Browse catalog and select product.
  3. Add product to cart and adjust quantity.
  4. Validate promotional coupon code.
  5. Place order via `/checkout/place-order`.
  6. Process SePay VietQR payment webhook (or simulation).
  7. Confirm order advances to `PROCESSING`.
  8. Admin packs order (`PACKED`).
  9. Admin dispatches order with tracking code (`SHIPPING`).
  10. Delivery completed (`DELIVERED`).
  11. User views order tracking timeline with recorded milestones.
- **Scenario 2 — Full POP NOW Blind Box Unbox-to-Shipment Journey**:
  1. Select collectible series and view available slots on tray.
  2. Reserve target box slot (status `RESERVED`, slot `HELD`).
  3. Generate checkout order for reservation.
  4. Confirm payment (status `PURCHASED`, slot `SOLD`).
  5. Unbox blind box online (status `UNBOXED`, item added to user's virtual cabinet).
  6. Request shipment of owned items from cabinet with shipping address.
  7. Dispatch order generated and linked to cabinet items.

---

## 5. Test Data Isolation & Idempotency Strategy

To ensure zero cross-test interference and reproducible runs:
1. **Dynamic Unique Identifiers**: All test artifacts (users, products, coupons, orders, reservations) use random UUID prefixes (`TEST-USR-...`, `E2E-COUPON-...`).
2. **Transactional & Teardown Cleanliness**: Tests operate within `@Transactional` boundaries where applicable, and test fixtures create their own clean isolated entities.
3. **No External Network Dependencies**: Webhooks (SePay) and Cloudinary media are tested via local payloads and internal service endpoints without hitting external third-party servers.

---

## 6. Execution Commands

Compile the test suite:
```powershell
cmd /c "set JAVA_HOME=C:\Users\hopl2\.jdks\temurin-25.0.3& .\mvnw.cmd test-compile"
```

Run all E2E test suites:
```powershell
cmd /c "set JAVA_HOME=C:\Users\hopl2\.jdks\temurin-25.0.3& .\mvnw.cmd test -Dtest=com.manguonmo.popworld.e2e.**.*Test"
```

Run a specific tier:
```powershell
# Tier 1
cmd /c "set JAVA_HOME=C:\Users\hopl2\.jdks\temurin-25.0.3& .\mvnw.cmd test -Dtest=Tier1*Test"

# Tier 2
cmd /c "set JAVA_HOME=C:\Users\hopl2\.jdks\temurin-25.0.3& .\mvnw.cmd test -Dtest=Tier2*Test"

# Tier 3
cmd /c "set JAVA_HOME=C:\Users\hopl2\.jdks\temurin-25.0.3& .\mvnw.cmd test -Dtest=Tier3*Test"

# Tier 4
cmd /c "set JAVA_HOME=C:\Users\hopl2\.jdks\temurin-25.0.3& .\mvnw.cmd test -Dtest=Tier4*Test"
```
