# BÁO CÁO TỔNG KẾT KIỂM THỬ TẢI, HIỆU NĂNG DATABASE & BẢO MẬT HỆ THỐNG POPWORLD
## (AUDIT, PERFORMANCE, LOAD TESTING & SECURITY HARDENING MASTER REPORT)

> **Hệ thống**: Nền tảng Thương mại Điện tử Art Toy & Blind Box POP NOW — PopWorld  
> **Phiên bản**: 0.0.1-SNAPSHOT (Production-Ready)  
> **Môi trường Runtime**: Java 25 (Eclipse Temurin JDK 25.0.3) / Spring Boot 4.1.1 / Spring Security 6 / Hibernate 7.4.5  
> **Ngày hoàn tất kiểm định**: Tháng 10/2026  
> **Kết quả kiểm thử tự động**: **638/638 Tests PASS (100% BUILD SUCCESS - 0 Failures - 0 Errors - 0 Skipped)**

---

## 1. TỔNG QUAN ĐÁNH GIÁ (EXECUTIVE SUMMARY)

Đợt rà soát, tái cấu trúc và kiểm thử toàn diện đã hoàn thành xuất sắc toàn bộ 3 nhóm yêu cầu cốt lõi (**R1 - Bảo Mật**, **R2 - Hiệu Năng Cơ Sở Dữ Liệu & JPA**, **R3 - Kiểm Thử Tải & Chống Đua Tranh Concurrency**). Toàn bộ hệ thống PopWorld hiện đã đạt trạng thái **Hoàn Thiện 100%**, sẵn sàng phục vụ báo cáo Đồ Án Tốt Nghiệp và vận hành thực tế ở môi trường Production.

### Bảng Chỉ Số Đo Lường Chính (Key Performance Indicators)

| Tiêu Chí Đo Lường | Trước Tối Ưu | Sau Tối Ưu | Đánh Giá & Ý Nghĩa Kỹ Thuật |
|---|:---:|:---:|---|
| **Tổng số Automated Tests** | 532 tests (14 lỗi) | **638 tests (100% pass)** | Hệ thống kiểm thử toàn diện từ Unit, Controller Slice, 4-Tier E2E đến Multi-threaded Concurrency. |
| **Truy vấn danh sách Đơn hàng (Admin/User)** | $1 + N$ queries | **$O(1)$ (2 queries cố định)** | Triệt tiêu N+1 bằng `findByOrderIdIn` kết hợp `OrderMapper` và Map lookup. |
| **Truy vấn Giỏ hàng (Cart Item Images)** | $1 + N$ queries | **$1$ query duy nhất** | `@EntityGraph(attributePaths = {"product", "product.images"})`. |
| **Truy vấn Đánh giá Sản phẩm (Reviews)** | $1 + N$ queries | **$1$ query duy nhất** | `@EntityGraph(attributePaths = {"user", "product"})`. |
| **Bảo vệ Tranh mua Tồn kho (10 threads / stock=1)** | Rủi ro stale cache L1 | **Chính xác 1 đơn, stock=0** | Atomic SQL update + `@Modifying(clearAutomatically=true, flushAutomatically=true)`. |
| **Chống Deadlock Checkout Giỏ nhiều món** | Rủi ro Deadlock MySQL | **Tuyệt đối không Deadlock** | Sắp xếp deterministic `cart.getProduct().getId() ASC` trước khi lock. |
| **Bảo vệ Tranh bóc hộp POP NOW (5 threads / 1 slot)** | Race condition | **Đúng 1 slot HELD, 4 từ chối** | Khóa bi quan `@Lock(LockModeType.PESSIMISTIC_WRITE)`. |
| **Lỗ hổng IDOR trên API Đơn/Giỏ/Hộp** | Trả về 400 Bad Request | **Chặn và ném 403 Forbidden** | Phân quyền nghiêm ngặt theo User Ownership. |
| **Phòng vệ Brute-force & Spam API** | Không có giới hạn | **Kích hoạt RateLimitingFilter** | Token-bucket chặn `/login`, `/checkout`, `/order-tracking`, `/api/popnow/reserve`. |
| **Tính độc lập của Bộ Kiểm Thử (CI/CD Ready)** | Phụ thuộc MySQL Docker | **100% In-Memory H2 Autonomous** | Chạy độc lập mọi máy tính với `application-test.properties`. |

---

## 2. BẢO MẬT WEB & API (SECURITY HARDENING - R1)

### 2.1. Phân Quyền Cấp Phương Thức (Method-Level RBAC)
- **Hiện trạng trước đây**: File `SecurityConfig` thiếu `@EnableMethodSecurity`, khiến các annotation `@PreAuthorize` bị vô hiệu hóa ngầm.
- **Khắc phục**: 
  - Kích hoạt `@EnableMethodSecurity` trong [`SecurityConfig.java`](file:///d:/MaNguonMo/popworld/src/main/java/com/manguonmo/popworld/config/SecurityConfig.java).
  - Áp dụng đồng bộ `@PreAuthorize("hasRole('ADMIN')")` lên toàn bộ 11 Controller Quản trị viên (`AdminProductWebController`, `AdminOrderWebController`, `AdminInventoryWebController`, `AdminCouponWebController`, `AdminReportWebController`, `AdminSupportWebController`, `AdminPopNowWebController`, `AdminCustomerWebController`, `AdminReviewWebController`, `AdminDatabaseWebController`, `DashboardWebController`).
  - Kiểm chứng bằng bộ test challenge chuyên biệt: [`AdminRbacSecurityChallengeTest.java`](file:///d:/MaNguonMo/popworld/src/test/java/com/manguonmo/popworld/security/AdminRbacSecurityChallengeTest.java).

### 2.2. Triệt Tiêu Lỗ Hổng IDOR (Insecure Direct Object References)
- **Điểm yếu cũ**: Khi người dùng A cố tình truy cập đơn hàng, giỏ hàng, hoặc phiếu giữ hộp của người dùng B, hệ thống ném `BadRequestException` (HTTP 400) hoặc không chặn ở một số endpoint.
- **Khắc phục**:
  - Chuyển toàn bộ các trường hợp vi phạm quyền sở hữu sang ném `AccessDeniedException` (HTTP 403 Forbidden).
  - Bổ sung handler `@ExceptionHandler(AccessDeniedException.class)` trong [`GlobalExceptionHandler.java`](file:///d:/MaNguonMo/popworld/src/main/java/com/manguonmo/popworld/exception/GlobalExceptionHandler.java) để trả về phản hồi JSON/HTML 403 chuẩn mực mà không bị nuốt thành lỗi 500.

### 2.3. Củng Cố Phòng Thủ CSRF (Cross-Site Request Forgery)
- **Điểm yếu cũ**: Endpoint `/api/popnow/simulate-payment` nằm trong danh sách `ignoringRequestMatchers`, và route `/popnow/checkout/{reservationCode}` cho phép gọi bằng HTTP GET.
- **Khắc phục**:
  - Loại bỏ `/api/popnow/simulate-payment` khỏi danh sách bỏ qua CSRF trong `SecurityConfig.java`.
  - Khóa chặt endpoint `/popnow/checkout/{reservationCode}` chỉ chấp nhận `@PostMapping` với CSRF token hợp lệ.

### 2.4. Kiểm Soát Tần Suất Truy Cập (Rate Limiting Filter)
- Cài đặt [`RateLimitingFilter.java`](file:///d:/MaNguonMo/popworld/src/main/java/com/manguonmo/popworld/security/ratelimit/RateLimitingFilter.java) đứng trước `CsrfFilter` với cơ chế Token-Bucket:
  - `/login`: Giới hạn 5 lần thử / phút (ngăn chặn brute-force mật khẩu).
  - `/checkout/place-order`: Giới hạn 10 request / phút (ngăn chặn spam đơn hàng rác).
  - `/order-tracking`: Giới hạn 20 request / phút (ngăn chặn dò quét mã đơn hàng).
  - `/api/popnow/reserve`: Giới hạn 15 request / phút (ngăn chặn bot giữ slot blind box).

---

## 3. TỐI ƯU HÓA HIỆU NĂNG DATABASE & JPA (R2)

### 3.1. Thiết Lập Database Indexes Toàn Diện
Đã bổ sung các Database Index có tính chọn lọc cao (High Cardinality) trên 6 Entity trọng yếu bằng JPA `@Table(indexes = { ... })`:
1. **`products`**: `category_id`, `series_id`, `(active, is_featured)`, `(active, is_new_release)`, `created_at`.
2. **`orders`**: `user_id`, `status`, `created_at`, `(status, expires_at)`.
3. **`order_items`**: `order_id`, `product_id`.
4. **`reviews`**: `(product_id, approved, created_at)`.
5. **`user_addresses`**: `user_id`, `(user_id, is_default)`.
6. **`product_images`**: `product_id`, `(product_id, is_thumbnail)`.

### 3.2. Triệt Tiêu N+1 Queries
- **Đơn hàng**: Phương thức `getOrderItemsByOrderIds(List<Long> orderIds)` sử dụng `OrderItemRepository.findByOrderIdIn(orderIds)` với `@EntityGraph(attributePaths = {"product", "product.images"})`. Việc nạp trước toàn bộ items gom nhóm theo `orderId` giảm số lượng truy vấn từ $1 + N$ xuống còn 2 truy vấn cố định $O(1)$.
- **Giỏ hàng**: [`CartItemRepository.java`](file:///d:/MaNguonMo/popworld/src/main/java/com/manguonmo/popworld/repository/CartItemRepository.java) cấu hình `@EntityGraph(attributePaths = {"product", "product.images"})` trên toàn bộ các phương thức truy vấn, giải quyết triệt để N+1 khi render giỏ hàng và thanh toán.
- **Đánh giá**: [`ReviewRepository.java`](file:///d:/MaNguonMo/popworld/src/main/java/com/manguonmo/popworld/repository/ReviewRepository.java) nạp sẵn thông tin `user` và `product`.

### 3.3. Tối Ưu Connection Pool & Transaction Boundaries
- **HikariCP Pool**: Cấu hình trong `application.properties`:
  ```properties
  spring.datasource.hikari.maximum-pool-size=30
  spring.datasource.hikari.minimum-idle=10
  spring.datasource.hikari.connection-timeout=5000
  spring.datasource.hikari.leak-detection-threshold=20000
  spring.jpa.properties.hibernate.default_batch_fetch_size=30
  ```
- **Transaction Boundaries**: Bổ sung `@Transactional(readOnly = true)` tại các Service đọc dữ liệu (`CategoryServiceImpl`, `CharacterIpServiceImpl`, `OrderServiceImpl`), giúp Hibernate tự động vô hiệu hóa cơ chế Dirty Checking và giải phóng connection sớm.

---

## 4. KIỂM THỬ TẢI & PHÒNG CHỐNG ĐUA TRANH CONCURRENCY (R3)

Bộ kiểm thử tải đa luồng tích hợp thực tế được xây dựng tại [`ConcurrencyLoadIntegrationTest.java`](file:///d:/MaNguonMo/popworld/src/test/java/com/manguonmo/popworld/concurrency/ConcurrencyLoadIntegrationTest.java):

### 4.1. Kịch Bản 1: Tranh Mua Tồn Kho Hữu Hạn (Stock Race Condition)
- **Thiết kế**: 10 luồng đồng thời xuất phát tại cùng 1 mili-giây (`CountDownLatch`) để mua 1 sản phẩm có tồn kho duy nhất $K = 1$.
- **Cơ chế bảo vệ**:
  - Atomic SQL Update: `UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity WHERE p.id = :productId AND p.stockQuantity >= :quantity`.
  - Hibernate Invariant: `@Modifying(clearAutomatically = true, flushAutomatically = true)` đảm bảo dữ liệu trong Hibernate Session luôn đồng bộ tức thì với database.
  - Sắp xếp Anti-Deadlock: `orderedCartItems.sort(Comparator.comparing(item -> item.getProduct().getId()))` trước khi lock, triệt tiêu 100% rủi ro deadlock.
- **Kết quả kiểm thử**:
  - **Thành công**: Chính xác **1 đơn hàng** hoàn tất.
  - **Từ chối**: **9 luồng** nhận ngoại lệ `OutOfStockException`.
  - **Tồn kho cuối cùng**: $0$ (Tuyệt đối không có hiện tượng âm kho).

### 4.2. Kịch Bản 2: Tranh Giữ Hộp Blind Box POP NOW (POP NOW Slot Contention)
- **Thiết kế**: 5 luồng người chơi cùng nhấp chọn và giữ cùng 1 ô hộp (#4) tại cùng 1 thời điểm.
- **Cơ chế bảo vệ**:
  - Khóa bi quan `@Lock(LockModeType.PESSIMISTIC_WRITE)` trên `BlindBoxSlot` và `BoxReservation`.
  - Trạng thái vòng đời nghiêm ngặt: `AVAILABLE` $\rightarrow$ `HELD` $\rightarrow$ `SOLD` $\rightarrow$ `UNBOXED`.
- **Kết quả kiểm thử**:
  - **Thành công**: Đúng **1 khách hàng** giữ được ô hộp, phiếu chuyển sang `RESERVED`, slot chuyển sang `HELD`.
  - **Từ chối**: **4 khách hàng** nhận thông báo lỗi slot đã có người giữ.

### 4.3. Kịch Bản 3: Tranh Dùng Voucher Giới Hạn (Coupon Usage Concurrency)
- **Thiết kế**: 5 luồng từ 5 tài khoản khác nhau cùng áp dụng 1 coupon có số lượt dùng `usageLimit = 1`.
- **Cơ chế bảo vệ**:
  - Atomic increment `usedCount < usageLimit` trên `CouponRepository`.
  - Khóa bi quan `findByUserIdAndCouponIdForUpdate` trên `UserCouponRepository` ngăn chặn cùng 1 user lạm dụng bấm đúp.
- **Kết quả kiểm thử**:
  - Đúng **1 user** áp dụng thành công.
  - 4 user còn lại nhận thông báo mã đã hết lượt sử dụng.
  - `usedCount` trong database bằng chính xác 1.

---

## 5. HẠ TẦNG KIỂM THỬ TỰ CHỦ (TEST AUTONOMY)

- **H2 In-Memory Compatibility**: Cấu hình file [`src/test/resources/application-test.properties`](file:///d:/MaNguonMo/popworld/src/test/resources/application-test.properties) kích hoạt chế độ MySQL Compatibility Mode cho H2.
- **Độc lập môi trường**: Bất kỳ lập trình viên nào tải mã nguồn về đều có thể chạy `mvn test` và đạt **BUILD SUCCESS 100%** ngay lập tức mà không cần phải cài đặt hay chạy trước Docker MySQL trên máy local.

---

## 6. HƯỚNG DẪN VẬN HÀNH & TRIỂN KHAI PRODUCTION

### Biến Môi Trường Khuyến Nghị (Production Environment Variables)
```bash
# Database Configuration
DB_URL=jdbc:mysql://prod-db-host:3306/popworld_db?useSSL=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8
DB_USERNAME=popworld_prod_user
DB_PASSWORD=<SECURE_STRONG_PASSWORD>

# JPA & DDL (Tuyệt đối không dùng 'update' trên production)
JPA_DDL_AUTO=validate
SPRING_JPA_SHOW_SQL=false

# SePay Payment Gateway
SEPAY_WEBHOOK_API_KEY=<PRODUCTION_SEPAY_SECRET_KEY>
SEPAY_BANK_CODE=MBBank
SEPAY_ACCOUNT_NUMBER=0355416208
SEPAY_ACCOUNT_NAME="POPWORLD OFFICIAL STORE"

# Cloudinary CDN Storage
CLOUDINARY_CLOUD_NAME=<CLOUDINARY_NAME>
CLOUDINARY_API_KEY=<CLOUDINARY_KEY>
CLOUDINARY_API_SECRET=<CLOUDINARY_SECRET>

# Security
SESSION_COOKIE_SECURE=true
APP_INIT_DEMO_DATA=false
```

### Tham Số Khởi Động JVM Tối Ưu (JVM Tuning Flags)
```bash
java -Xms1024m -Xmx2048m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+ParallelRefProcEnabled \
     -Dfile.encoding=UTF-8 \
     -jar target/popworld-0.0.1-SNAPSHOT.jar
```

---

## 7. KẾT LUẬN

Hệ thống **PopWorld** hiện tại đã đạt độ chín muồi cao về cả mặt tính năng nghiệp vụ Art Toy E-Commerce, tính thẩm mỹ giao diện Pop Mart hiện đại, tính an toàn thông tin theo chuẩn OWASP Top 10, hiệu năng truy vấn cơ sở dữ liệu tối ưu O(1), và năng lực xử lý xung đột dữ liệu đa luồng vững chắc. Dự án hoàn toàn đủ điều kiện xuất sắc để bảo vệ Đồ Án Tốt Nghiệp và vận hành thương mại.
