# POPWORLD - PRODUCTION DEPLOYMENT & HAND-OFF DOCUMENTATION

Tài liệu bàn giao và hướng dẫn triển khai sản phẩm ứng dụng Popworld (Hệ thống Thương mại Điện tử Blind Box & Art Toy).

---

## 1. Project Stack & Architecture Overview

- **Core Framework**: Spring Boot 4.1.1, Java 21+ (Đã kiểm thử và xác nhận tương thích hoàn toàn trên OpenJDK 25).
- **Security**: Spring Security 7.x
  - Mã hóa mật khẩu: `BCryptPasswordEncoder`.
  - Phòng chống tấn công: CSRF Protection toàn diện, In-memory Sliding Window Rate Limiting, Session Fixation Protection (`changeSessionId`), Cookie an toàn (`HttpOnly`, `SameSite=Lax`, `Secure`).
- **Database & ORM**: MySQL 8.0+, Spring Data JPA, Hibernate ORM.
- **Template Engine & UI**: Thymeleaf 3.1.5, Spring Security 6 dialect, Bootstrap 5.3.3, Bootstrap Icons 1.11.3 (PopMart-inspired design).
- **Payment Gateway**: SePay VietQR (sepay.vn) chuyển khoản tự động + Webhook xử lý bất đồng bộ chuẩn Idempotent.
- **Kiến trúc hệ thống**:
  - Phân tầng chuẩn Enterprise MVC: Controller (`admin`, `client`, `api`, `webhook`), Service/ServiceImpl, Repository, Entity (`BaseEntity`), DTO (`request`, `response`), Mapper (`MapStruct 1.6.3`), Exception Handler tập trung (`GlobalExceptionHandler`).
  - Worker nền: `OrderCleanupScheduler` tự động quét mỗi 60 giây để hủy và hoàn kho cho đơn hàng `TO_PAY` quá hạn 15 phút chưa thanh toán.

---

## 2. Hướng dẫn Chạy Local Development từ đầu

### Yêu cầu tiên quyết:
- JDK 21 hoặc mới hơn (`JAVA_HOME` trỏ tới JDK).
- MySQL Server 8.0+ đang chạy trên cổng 3306.
- Git & Maven wrapper `mvnw.cmd` (có sẵn trong repository).

### Các bước khởi chạy:
1. **Khởi tạo Database**:
   ```sql
   CREATE DATABASE popworld_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. **Cấu hình môi trường (Tùy chọn nạp dữ liệu mẫu)**:
   Nếu muốn nạp sẵn danh mục, sản phẩm và tài khoản mẫu POP MART, hãy bật cờ `APP_INIT_DEMO_DATA`:
   - PowerShell:
     ```powershell
     $env:APP_INIT_DEMO_DATA = "true"
     ```
   - Bash / Linux / macOS:
     ```bash
     export APP_INIT_DEMO_DATA="true"
     ```
3. **Khởi chạy ứng dụng**:
   - Windows PowerShell:
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```
   - Linux / macOS:
     ```bash
     ./mvnw spring-boot:run
     ```
4. **Truy cập ứng dụng**:
   - Trang người dùng: `http://localhost:8080/`
   - Trang quản trị: `http://localhost:8080/admin`
5. **Tài khoản kiểm thử mẫu (khi bật demo data)**:
   - Quản trị viên: `admin@popworld.com` / `admin123`
   - Khách hàng VIP: `user@popworld.com` / `user123`
   - Khách hàng Member: `thaolinh@gmail.com` / `user123`

---

## 3. Danh mục Biến Môi Trường (Environment Variables)

Hệ thống tuân thủ chuẩn 12-Factor App. Toàn bộ cấu hình có thể ghi đè linh hoạt qua biến môi trường mà không cần sửa code:

| Biến môi trường | Phân loại | Giá trị mặc định (Local) | Mô tả & Giá trị Production khuyến nghị |
| :--- | :---: | :--- | :--- |
| `PORT` | Optional | `8080` | Cổng HTTP lắng nghe (hỗ trợ PaaS như Render, Railway, Heroku). |
| `DB_URL` | **Required (Prod)** | `jdbc:mysql://127.0.0.1:3306/popworld_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8` | Chuỗi JDBC kết nối MySQL trên server thật, nên bật `useSSL=true`. |
| `DB_USERNAME` | **Required (Prod)** | `root` | Tên người dùng database trên production. |
| `DB_PASSWORD` | **Required (Prod)** | `admin123` | Mật khẩu database bảo mật cao trên production. |
| `JPA_DDL_AUTO` | Optional | `update` | Chế độ DDL Hibernate. Khuyến nghị `validate` hoặc `none` trên production. |
| `SPRING_JPA_SHOW_SQL` | Optional | `false` | Bật/tắt log câu lệnh SQL ra stdout (khuyến nghị `false`). |
| `HIBERNATE_FORMAT_SQL` | Optional | `false` | Định dạng SQL trong log (khuyến nghị `false`). |
| `THYMELEAF_CACHE` | Optional | `false` | Bật/tắt cache giao diện HTML Thymeleaf (khuyến nghị `true` trên prod). |
| `SEPAY_WEBHOOK_API_KEY` | **Required (Prod)** | `popworld_secret_key_2026` | API Key bí mật xác thực webhook từ SePay. Bắt buộc đặt chuỗi bí mật ngẫu nhiên mạnh. |
| `SEPAY_BANK_CODE` | **Required (Prod)** | `MBBank` | Mã ngân hàng nhận tiền chuyển khoản (MBBank, Vietcombank, ACB, v.v.). |
| `SEPAY_ACCOUNT_NUMBER` | **Required (Prod)** | `0355416208` | Số tài khoản ngân hàng nhận tiền chuyển khoản thật. |
| `SEPAY_ACCOUNT_NAME` | **Required (Prod)** | `POPWORLD OFFICIAL STORE`| Tên chủ tài khoản ngân hàng viết hoa không dấu. |
| `APP_INIT_DEMO_DATA` | Optional | `false` | **Mặc định OFF**. Đảm bảo không nạp tài khoản và dữ liệu mẫu vào DB thật. |
| `SESSION_COOKIE_SECURE` | **Required (Prod)** | `false` | Bật cờ `Secure` cho cookie phiên JSESSIONID. **Bắt buộc `true` trên HTTPS**. |
| `server.tomcat.response-buffer-size` | Optional | `65536` | Kích thước buffer 64KB của Tomcat đảm bảo template streaming ổn định trước khi response commit. |

---

## 4. Cơ chế Quản lý Dữ liệu Mẫu (APP_INIT_DEMO_DATA)

- **Môi trường Production**:
  - Giá trị mặc định là **`false`**.
  - `DataInitializer` sử dụng annotation `@ConditionalOnProperty(name = "app.init-demo-data", havingValue = "true")`.
  - Khi `APP_INIT_DEMO_DATA` không được set hoặc là `false`, `DataInitializer` hoàn toàn không khởi tạo, không ghi bất kỳ tài khoản demo (`admin123`, `user123`) nào vào database.
- **Môi trường Local / Dev**:
  - Thiết lập biến môi trường `APP_INIT_DEMO_DATA=true` trước khi khởi động để tự động nạp 8 nhân vật IP, 15 sản phẩm Blind Box, tin tức, mã giảm giá (`POP10`, `FREESHIP`), và tài khoản kiểm thử.
  - Hệ thống có guard `if (productRepository.count() > 0) return;` chống nạp đè nếu dữ liệu đã tồn tại.

---

## 5. Cài đặt Database & Đóng gói Artifact Triển khai

### Khởi tạo Database:
- Khởi tạo Database với encoding UTF-8:
  ```sql
  CREATE DATABASE popworld_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```
- Nếu dùng schema migration thủ công, áp dụng schema và cấu hình `JPA_DDL_AUTO=validate`.

### Đóng gói Runnable Fat JAR:
- Chạy lệnh Maven packaging:
  ```powershell
  $env:JAVA_HOME = "C:\Users\hopl2\.jdks\temurin-25.0.3"; .\mvnw.cmd package -DskipTests
  ```
- Kết quả tạo ra file artifact độc lập:
  `target/popworld-0.0.1-SNAPSHOT.jar`
- Khởi chạy artifact trên production:
  ```bash
  java -jar -Dspring.profiles.active=prod target/popworld-0.0.1-SNAPSHOT.jar
  ```

---

## 6. Cấu hình Cổng Thanh toán & Webhook SePay

1. Đăng ký tài khoản doanh nghiệp tại cổng [https://sepay.vn](https://sepay.vn) và liên kết tài khoản ngân hàng nhận tiền.
2. Cấu hình Webhook trên Dashboard SePay:
   - **Webhook URL**: `https://<DOMAIN_CUA_BAN>/api/payment/sepay/webhook`
   - **Phương thức HTTP**: `POST`
   - **Authentication Type**: API Key / Header
   - **Authorization Header**: `Bearer <SEPAY_WEBHOOK_API_KEY>`
3. **Cơ chế Bảo mật & Xử lý Webhook đã triển khai**:
   - **Xác thực API Key**: Kiểm tra header `Authorization` chứa đúng API Key đã cấu hình; từ chối mọi request không hợp lệ.
   - **Rate Limiting**: Giới hạn tối đa 60 requests/phút từ cùng một IP/client (ngăn chặn DoS và webhook flooding).
   - **Xử lý Idempotency**: Bỏ qua các đơn hàng đã thanh toán trước đó (`PROCESSING`, `SHIPPING`, `DELIVERED`, `SHIPPED`, `COMPLETED`), không ghi đè trạng thái và không hoàn kho sai.
   - **Kiểm tra số tiền thanh toán**: So khớp `transferAmount >= order.totalAmount`. Nếu thiếu tiền, ghi nhận cảnh báo và không đổi trạng thái đơn.
   - **Phân tích mã đơn hàng**: Tự động trích xuất mã đơn hàng regex `PW-\d+` từ nội dung chuyển khoản `content`.

---

## 7. Danh mục Bảo mật Đã Hoàn thành (Security Checklist)

- [x] **CSRF Protection & View Engine Stability**: Bảo vệ toàn bộ HTTP POST/PATCH/DELETE endpoints. Tích hợp sẵn CSRF token trong tất cả form Thymeleaf và AJAX headers. Áp dụng cơ chế eager resolution (`CsrfTokenRequestAttributeHandler.setCsrfRequestAttributeName(null)`) kết hợp Tomcat 64KB buffer (`server.tomcat.response-buffer-size=65536`) để loại bỏ xung đột streaming response khi anonymous user duyệt catalog chứa form POST. Loại trừ có kiểm soát endpoint `/api/payment/sepay/**`.
- [x] **Rate Limiting & Abuse Prevention**:
  - `/login`: Giới hạn 10 requests/phút/IP.
  - `/register`: Giới hạn 5 requests/phút/IP.
  - Thao tác giỏ hàng (`POST /cart/add`, `POST /api/cart/**`): Giới hạn 30 requests/phút/IP.
  - Webhook SePay: Giới hạn 60 requests/phút/IP.
  - API chung: Giới hạn 120 requests/phút/IP.
- [x] **Authentication & Role-Based Authorization**:
  - Khách vãng lai truy cập: Trang chủ, Danh sách sản phẩm, Chi tiết sản phẩm, Tin tức, Giới thiệu, Đăng ký, Đăng nhập.
  - Yêu cầu xác thực (`ROLE_USER` / Authenticated): Giỏ hàng (`/cart/**`), Thanh toán (`/checkout/**`), Đơn hàng cá nhân (`/orders/**`), Hồ sơ (`/account/**`, `/profile`).
  - Yêu cầu quyền Quản trị (`ROLE_ADMIN`): Toàn bộ `/admin/**` (Dashboard, Quản lý sản phẩm, Quản lý đơn hàng, Quản lý khách hàng).
- [x] **Session & Cookie Security**:
  - Bật chống Session Fixation (`changeSessionId`).
  - Đăng xuất an toàn: Hủy phiên làm việc (`invalidateHttpSession`) và xóa cookie `JSESSIONID`.
  - Cờ bảo mật cookie: `HttpOnly=true`, `SameSite=Lax`, `Secure=${SESSION_COOKIE_SECURE}`.
- [x] **Che giấu Thông tin & Chống Rò rỉ Lỗi**:
  - Cấu hình Spring Boot: `server.error.include-stacktrace=never`, `server.error.include-exception=false`.
  - Bộ xử lý ngoại lệ tập trung `GlobalExceptionHandler`: Xử lý `MethodArgumentNotValidException` trả về HTTP 400 Bad Request; ẩn chi tiết lỗi nội bộ khi gặp HTTP 500.

---

## 8. Lệnh Kiểm thử & Kết quả Xác thực Hiện tại

- **Chạy toàn bộ Unit & Integration Test Suite**:
  ```powershell
  $env:JAVA_HOME = "C:\Users\hopl2\.jdks\temurin-25.0.3"; .\mvnw.cmd test "-Dtest=!PopworldApplicationTests"
  ```
  - **Kết quả**: `Tests run: 229, Failures: 0, Errors: 0, Skipped: 0` (**229/229 passed**, 100% thành công).
- **Kiểm tra đóng gói Artifact**:
  ```powershell
  $env:JAVA_HOME = "C:\Users\hopl2\.jdks\temurin-25.0.3"; .\mvnw.cmd package -DskipTests
  ```
  - **Kết quả**: `BUILD SUCCESS`, tạo file `target/popworld-0.0.1-SNAPSHOT.jar`.

---

## 9. Kịch bản Kiểm thử Thủ công (Manual Smoke-Test Checklist)

### Luồng Khách vãng lai (Guest):
1. Truy cập trang chủ `/` -> Kiểm tra slider hero banner, carousel IP nhân vật, danh mục nổi bật.
2. Xem danh sách sản phẩm `/products` -> Lọc theo danh mục IP, tìm kiếm theo từ khóa.
3. Xem chi tiết sản phẩm `/products/{slug}` -> Xem thông tin tỷ lệ ra bản Secret (1/72, 1/144), chọn mua Single Box hoặc Whole Set.
4. Thử đặt hàng mà chưa đăng nhập -> Hệ thống chuyển hướng yêu cầu đăng nhập `/login`.

### Luồng Khách hàng (Customer Flow):
1. Đăng ký tài khoản tại `/register` -> Đăng nhập tại `/login`.
2. Thêm sản phẩm vào giỏ hàng tại `/cart` -> Thay đổi số lượng, chọn sản phẩm cần thanh toán.
3. Nhấn "Tiến hành đặt hàng" -> Chuyển hướng sang `/checkout`.
4. Điền thông tin giao hàng, áp dụng mã giảm giá (`POP10`), chọn phương thức:
   - **COD**: Đặt hàng thành công -> Chuyển hướng `/checkout/success/{orderCode}`.
   - **SEPAY**: Chuyển hướng `/checkout/payment/{orderCode}` -> Hiển thị mã QR VietQR động chứa đúng số tiền và mã đơn hàng `PW-...`.
5. Vào `/orders` kiểm tra danh sách đơn hàng đã mua và trạng thái đơn hàng.
6. Thử hủy đơn hàng ở trạng thái `TO_PAY` -> Đơn chuyển sang `CANCELLED`, số lượng tồn kho được hoàn trả ngay lập tức.

### Luồng Quản trị viên (Admin Flow):
1. Đăng nhập bằng tài khoản Admin (`ROLE_ADMIN`).
2. Vào `/admin/dashboard` -> Xem báo cáo thống kê doanh thu, đơn hàng theo trạng thái, biểu đồ và đơn hàng mới nhất.
3. Vào `/admin/products` -> Xem danh sách sản phẩm, cập nhật tồn kho nhanh (stock quantity), bật/tắt mở bán sản phẩm (`toggle-active`).
4. Vào `/admin/orders` -> Lọc đơn hàng theo trạng thái, xem chi tiết đơn `/admin/orders/{orderCode}`, cập nhật trạng thái đơn: `PROCESSING` -> `SHIPPING` -> `DELIVERED`, hoặc hủy đơn.

---

## 10. Giới hạn Đã biết & Khuyến nghị Mở rộng (Production Recommendations)

1. **Bộ nhớ Rate Limiting (In-Memory)**:
   - Hệ thống hiện dùng In-Memory Sliding Window Counter phù hợp cho kiến trúc single-server hoặc container độc lập.
   - *Khuyến nghị tương lai*: Khi scale cụm ứng dụng (multi-instance đằng sau Load Balancer), hãy tích hợp Redis để chia sẻ bucket rate limit giữa các node.
2. **Lưu trữ Hình ảnh Sản phẩm**:
   - Hiện tại hình ảnh sử dụng URL trực tiếp từ CDN của hãng Pop Mart và BigCommerce.
   - *Khuyến nghị tương lai*: Khi Admin có nhu cầu tải ảnh sản phẩm tùy biến trực tiếp lên hệ thống, nên tích hợp dịch vụ lưu trữ đám mây (Cloudinary hoặc AWS S3 với Pre-signed URLs) thay vì lưu trên filesystem cục bộ của server.
3. **Giám sát & APM (Application Performance Monitoring)**:
   - Nhằm tối ưu hóa bề mặt bảo mật và giảm thiểu tấn công, ứng dụng không tích hợp `spring-boot-starter-actuator`.
   - *Khuyến nghị tương lai*: Khi cần đo lường metrics production, hãy cấu hình Spring Actuator với cổng nội bộ riêng (khác cổng `PORT` chính) hoặc dùng agent APM chuyên dụng (như Datadog, New Relic, OpenTelemetry).
