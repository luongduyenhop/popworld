# PopWorld E2E Test Suite Readiness Guide (TEST_READY.md)

## 1. Executive Summary & Test Suite Status

The **PopWorld End-to-End (E2E) Test Suite** is fully implemented, verified, and operational. It covers the entire lifecycle of the PopWorld platform across Web MVC, REST API, Business Service, Spring Security, and Persistence layers.

| Metric | Details |
|---|---|
| **Target Application** | PopWorld E-Commerce & POP NOW Platform |
| **Runtime Engine** | Java 25 (Eclipse Temurin JDK 25.0.3), Spring Boot 4.1.1, MySQL 8.0 (Docker) |
| **Total Test Classes** | 6 classes (Base harness + 5 tier test suites) |
| **Total Test Cases** | **36 exhaustive E2E tests** |
| **Pass Rate** | **100% PASS (36/36 tests passed)** |
| **Test Execution Engine** | Maven Surefire 3.5.6, JUnit 5 Jupiter, AssertJ, Spring MockMvc |

---

## 2. Four-Tier Coverage Matrix & Verification Inventory

### Tier 1: Feature Coverage & Core Security (14 Tests)

#### A. Core Business Feature Coverage (`Tier1FeatureCoverageE2ETest`) — 4 Tests
| Test Code | Scenario & Verification Focus | Result |
|---|---|---|
| `T1-FEAT-01` | Trừ tồn kho tức thời (Immediate Stock Deduction) khi đặt hàng COD | **PASS** |
| `T1-FEAT-02` | Khóa slot vật lý và trừ tồn kho tạm thời (TTL 5 phút) khi giữ hộp POP NOW | **PASS** |
| `T1-FEAT-03` | Tính chiết khấu phần trăm có giới hạn trần (Percent Discount with Cap) | **PASS** |
| `T1-FEAT-04` | Tính chiết khấu tiền cố định (Fixed Amount Discount) | **PASS** |

#### B. Security Defenses & Endpoints (`Tier1SecurityEndpointsE2ETest`) — 10 Tests
| Test Code | Scenario & Verification Focus | Result |
|---|---|---|
| `T1-SEC-01` | Chặn khách vãng lai (Anonymous) truy cập `/admin/**` -> 302 Redirect to `/login` | **PASS** |
| `T1-SEC-02` | Chặn người dùng thường (`ROLE_USER`) truy cập `/admin/**` -> HTTP 403 Forbidden | **PASS** |
| `T1-SEC-03` | Cấp phép truy cập Quản trị viên (`ROLE_ADMIN`) vào `/admin/**` -> HTTP 200 OK | **PASS** |
| `T1-SEC-04` | Chặn khách vãng lai truy cập `/cart`, `/checkout`, `/popnow/cabinet` -> 302 Redirect | **PASS** |
| `T1-SEC-05` | Ngăn chặn lỗ hổng IDOR khi xem chi tiết đơn hàng của người dùng khác -> 403 / 404 | **PASS** |
| `T1-SEC-06` | Ngăn chặn IDOR khi thao tác trên giỏ hàng (Cart Item) của người khác | **PASS** |
| `T1-SEC-07` | Ngăn chặn IDOR khi mở hộp (Unbox) phiếu giữ hộp của người khác | **PASS** |
| `T1-SEC-08` | Chặn tấn công giả mạo yêu cầu chéo CSRF trên endpoint POST nhạy cảm -> HTTP 403 | **PASS** |
| `T1-SEC-09` | Giới hạn tần suất (Rate Limiting) trên API thử mã giảm giá `/api/coupons/apply` -> HTTP 429 | **PASS** |
| `T1-SEC-10` | Giới hạn tần suất đăng ký tài khoản mới `/register` -> HTTP 429 Too Many Requests | **PASS** |

---

### Tier 2: Boundary Conditions & State Transitions (17 Tests)

#### A. Boundary & Corner Cases (`Tier2BoundaryCornerCasesE2ETest`) — 9 Tests
| Test Code | Scenario & Verification Focus | Result |
|---|---|---|
| `T2-BOUND-01` | Thêm vào giỏ khi tồn kho bằng 0 (Zero Stock) -> OutOfStockException | **PASS** |
| `T2-BOUND-02` | Mua số lượng vượt quá tồn kho khả dụng (Requested > Stock) -> OutOfStockException | **PASS** |
| `T2-BOUND-03` | Thêm giỏ hàng với số lượng không hợp lệ (quantity <= 0) -> BadRequestException | **PASS** |
| `T2-BOUND-04` | Thêm giỏ hàng sản phẩm đã ngừng kinh doanh (`active = false`) -> BadRequestException | **PASS** |
| `T2-BOUND-05` | Áp dụng mã giảm giá đã hết hạn sử dụng (`expiredDate < now`) -> BadRequestException | **PASS** |
| `T2-BOUND-06` | Áp dụng mã giảm giá chưa tới ngày hiệu lực (`startDate > now`) -> BadRequestException | **PASS** |
| `T2-BOUND-07` | Áp dụng mã giảm giá đã cạn lượt sử dụng (`usageLimit = usedCount`) -> BadRequestException | **PASS** |
| `T2-BOUND-08` | Áp dụng mã giảm giá khi chưa đạt giá trị đơn tối thiểu (`subtotal < minOrderAmount`) -> BadRequest | **PASS** |
| `T2-BOUND-09` | Kiểm tra tính bảo vệ của Coupon Engine trước input bất thường (Null/Empty/Non-existent) | **PASS** |

#### B. State Machine Transitions (`Tier2StateTransitionsE2ETest`) — 8 Tests
| Test Code | Scenario & Verification Focus | Result |
|---|---|---|
| `T2-TRANS-01` | Chống hủy đơn hàng 2 lần (Double Cancel Prevention) -> BadRequestException | **PASS** |
| `T2-TRANS-02` | Chặn hủy đơn hàng đã quá hạn thanh toán (`EXPIRED`) -> BadRequestException | **PASS** |
| `T2-TRANS-03` | Khách hàng chỉ được hủy đơn ở trạng thái `TO_PAY` (Chặn hủy khi đã `PROCESSING`/`PACKED`) | **PASS** |
| `T2-TRANS-04` | Admin đóng gói (`PACKED`) chỉ hợp lệ từ đơn `PROCESSING` -> BadRequestException nếu sai | **PASS** |
| `T2-TRANS-05` | Admin vận chuyển (`SHIPPING`) chỉ hợp lệ từ đơn `PACKED` / `PROCESSING` | **PASS** |
| `T2-TRANS-06` | Admin hoàn tất (`DELIVERED`) chỉ hợp lệ từ đơn `SHIPPING` | **PASS** |
| `T2-TRANS-07` | Chặn bóc hộp online khi chưa hoàn tất thanh toán (`status != PURCHASED`) | **PASS** |
| `T2-TRANS-08` | Chặn bóc hộp phiếu giữ hộp đã hết hạn (`EXPIRED`) hoặc đã hủy (`CANCELLED`) | **PASS** |

---

### Tier 3: Cross-Feature Combinations (`Tier3CrossFeatureCombinationsE2ETest`) — 3 Tests
| Test Code | Scenario & Verification Focus | Result |
|---|---|---|
| `T3-COMB-01` | Đặt hàng kết hợp mã giảm giá + Tồn kho thấp -> Hủy đơn khôi phục chính xác cả tồn kho và mã | **PASS** |
| `T3-COMB-02` | Giữ hộp POP NOW -> Khởi tạo đơn thanh toán -> Xác nhận thanh toán -> Mở hộp (Unbox Idempotent) | **PASS** |
| `T3-COMB-03` | Giỏ hàng nhiều món + Chọn một phần (Partial Selection) -> Chỉ đặt các món đã chọn, món chưa chọn giữ nguyên | **PASS** |

---

### Tier 4: Real-World Application Scenarios (`Tier4RealWorldScenariosE2ETest`) — 2 Tests
| Test Code | Scenario & Verification Focus | Result |
|---|---|---|
| `T4-SCEN-01` | **Hành trình mua sắm E-Commerce hoàn chỉnh 8 bước**:<br>1. Đăng nhập & xem hàng<br>2. Thêm giỏ hàng<br>3. Áp dụng mã giảm giá & tạo đơn `TO_PAY`<br>4. Nhận Webhook VietQR SePay đối soát số tiền và chuyển sang `PROCESSING`<br>5. Kho xác nhận đóng gói `PACKED`<br>6. Bàn giao đơn vị vận chuyển ViettelPost `SHIPPING`<br>7. Giao hàng thành công `DELIVERED`<br>8. Đối soát Timeline ghi nhận đầy đủ 5 trạng thái liên tục | **PASS** |
| `T4-SCEN-02` | **Hành trình mở hộp Art Toy POP NOW & giao hàng tận nhà 6 bước**:<br>1. Người chơi đăng nhập & chọn sản phẩm Blind Box<br>2. Giữ slot số 5 trên khay trực tiếp<br>3. Tạo đơn hàng thanh toán SePay gửi vào Tủ đồ ảo (`POP_NOW_CABINET`)<br>4. Webhook SePay ghi nhận tiền & xác nhận thanh toán<br>5. Bóc hộp trực tuyến (Reveal Art Toy) -> Cập nhật trạng thái `IN_CABINET`<br>6. Yêu cầu giao hàng từ Tủ đồ ảo về địa chỉ nhà (`POP_NOW_SHIP`) -> Chuyển vật phẩm sang `REQUESTED_SHIPPING` | **PASS** |

---

## 3. Test Runner Instructions

### 3.1. Yêu cầu môi trường (Prerequisites)
1. **Java Runtime**: JDK 21+ (Khuyên dùng Eclipse Temurin JDK 25.0.3).
2. **Cơ sở dữ liệu**: Docker MySQL 8.0 container đang chạy tại `127.0.0.1:3306`, schema `popworld_db`, user `root`, password `root`.
   - Lệnh kiểm tra Docker:
     ```bash
     docker ps --filter "name=popworld-mysql"
     ```

### 3.2. Lệnh chạy toàn bộ Test Suite (Full E2E Run)
```powershell
cmd /c "set JAVA_HOME=C:\Users\hopl2\.jdks\temurin-25.0.3& .\mvnw.cmd test -Dtest=Tier1FeatureCoverageE2ETest,Tier1SecurityEndpointsE2ETest,Tier2BoundaryCornerCasesE2ETest,Tier2StateTransitionsE2ETest,Tier3CrossFeatureCombinationsE2ETest,Tier4RealWorldScenariosE2ETest"
```

### 3.3. Lệnh chạy theo từng Tier độc lập

#### Chạy Tier 1 (Core Features + Security):
```powershell
cmd /c "set JAVA_HOME=C:\Users\hopl2\.jdks\temurin-25.0.3& .\mvnw.cmd test -Dtest=Tier1FeatureCoverageE2ETest,Tier1SecurityEndpointsE2ETest"
```

#### Chạy Tier 2 (Boundaries + State Transitions):
```powershell
cmd /c "set JAVA_HOME=C:\Users\hopl2\.jdks\temurin-25.0.3& .\mvnw.cmd test -Dtest=Tier2BoundaryCornerCasesE2ETest,Tier2StateTransitionsE2ETest"
```

#### Chạy Tier 3 (Cross-Feature Combinations):
```powershell
cmd /c "set JAVA_HOME=C:\Users\hopl2\.jdks\temurin-25.0.3& .\mvnw.cmd test -Dtest=Tier3CrossFeatureCombinationsE2ETest"
```

#### Chạy Tier 4 (Real-World End-to-End Scenarios):
```powershell
cmd /c "set JAVA_HOME=C:\Users\hopl2\.jdks\temurin-25.0.3& .\mvnw.cmd test -Dtest=Tier4RealWorldScenariosE2ETest"
```

---

## 4. Báo cáo khiếm khuyết sản phẩm phát hiện được (Defect Escalation)

Trong quá trình xây dựng bộ kiểm thử E2E cấp cao, Đội ngũ Kiểm thử ghi nhận khiếm khuyết sau trong mã nguồn nghiệp vụ:

### ⚠️ Defect Report: `LazyInitializationException` khi Khách hàng hủy đơn hàng có áp dụng mã giảm giá
- **Vị trí**: `com.manguonmo.popworld.service.impl.OrderServiceImpl.java:330`
- **Mức độ nghiêm trọng**: **HIGH** (Gây lỗi HTTP 500 khi khách hàng nhấn nút Hủy đơn trên giao diện Web / API).
- **Nguyên nhân kỹ thuật**:
  - Khi đơn hàng có mã giảm giá (`order.getCoupon() != null`), hàm `cancelOrder()` gọi `couponService.releaseCoupon(couponId, userId)`.
  - Trong `CouponRepository.java`, phương thức `decreaseUsedCount` được gắn nhãn `@Modifying(clearAutomatically = true)`.
  - Thuộc tính `clearAutomatically = true` thực thi `entityManager.clear()`, đẩy toàn bộ thực thể hiện có trong Hibernate Session (bao gồm cả `order` và proxy `order.getUser()`) sang trạng thái **Detached**.
  - Tại dòng 330:
    ```java
    recordTimeline(savedOrder, "TO_PAY", "CANCELLED", "Khách hàng hủy đơn hàng", 
        (order.getUser() != null && order.getUser().getFullName() != null && !order.getUser().getFullName().isBlank() ? order.getUser().getFullName() : "Khách hàng"), 
        cancelNote);
    ```
    Phương thức truy xuất `order.getUser().getFullName()` trên proxy bị Detached, dẫn đến văng ngoại lệ `org.hibernate.LazyInitializationException: Could not initialize proxy [com.manguonmo.popworld.entity.User] - no session`.
- **Khuyến nghị khắc phục cho Developer**:
  - Cách 1: Trong `CouponRepository.java`, bỏ tham số `clearAutomatically = true` ở `decreaseUsedCount` và `increaseUsedCount` (hoặc chỉ clear khi cần thiết).
  - Cách 2: Trong `OrderServiceImpl.cancelOrder`, lưu `String actorName = (order.getUser() != null && order.getUser().getFullName() != null) ? ...` vào biến cục bộ **trước khi** gọi `couponService.releaseCoupon`.
  - Cách 3: Sử dụng `order.getRecipientName()` thay vì điều hướng qua `order.getUser().getFullName()`.
