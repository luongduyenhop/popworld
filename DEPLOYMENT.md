# POPWORLD - PRODUCTION DEPLOYMENT & OPERATION RUNBOOK

> **Tài liệu Bàn Giao & Hướng Dẫn Vận Hành**: Production Runbook  
> **Kiến trúc Triển khai**: Containerized Docker Multi-stage + MySQL 8.0 + Cloudflare Edge Tunnel  
> **Framework & Runtime**: Java 21+ / Spring Boot 4.x / Thymeleaf / Cloudinary CDN / SePay VietQR  

---

## 1. TỔNG QUAN KIẾN TRÚC PRODUCTION

Hệ thống `PopWorld` được thiết kế theo tiêu chuẩn **12-Factor App** hiện đại, đóng gói độc lập qua Docker Container, cho phép khởi chạy trên bất kỳ môi trường Linux VPS hay đám mây nào với duy nhất một lệnh:

```
[ Khách Hàng / Quản Trị Viên / SePay Webhook ]
                     │
                     ▼ (HTTPS Port 443)
┌────────────────────────────────────────────────────────┐
│ CLOUDFLARE EDGE NETWORK & WAF                          │ -> SSL Miễn Phí, Chống DDoS, Lọc Bot
└────────────────────┬───────────────────────────────────┘
                     │ (Encrypted Tunnel / Không cần mở Port VPS)
                     ▼
┌────────────────────────────────────────────────────────┐
│ CLOUDFLARE TUNNEL (cloudflared service trên VPS)       │
└────────────────────┬───────────────────────────────────┘
                     │ (HTTP localhost:8080)
                     ▼
┌────────────────────────────────────────────────────────┐
│ DOCKER BRIDGE NETWORK: popworld_network                │
│                                                        │
│  ┌─────────────────────────┐  ┌─────────────────────┐  │
│  │ Container: popworld-app │  │ Container: mysql    │  │
│  │ (Spring Boot 4.1.1 JRE) │◄─┤ (MySQL 8.0 Server)  │  │
│  │ Healthcheck: /api/health│  │ Volume: mysql_data  │  │
│  └───────────┬─────────────┘  └─────────────────────┘  │
└──────────────┼─────────────────────────────────────────┘
               ▼
┌────────────────────────────────────────────────────────┐
│ CLOUDINARY MEDIA CDN (Lưu trữ ảnh sản phẩm Art Toy)    │
└────────────────────────────────────────────────────────┘
```

---

## 2. KỊCH BẢN A: TRIỂN KHAI TRÊN CLOUD VPS VỚI DOCKER COMPOSE & CLOUDFLARE (KHUYẾN NGHỊ SỐ 1)

Phương án tối ưu nhất về chi phí, hiệu năng, sự độc lập và tính chuyên nghiệp cho đồ án tốt nghiệp và sản phẩm thực tế.

### Yêu Cầu Máy Chủ VPS:
- **Hệ điều hành**: Ubuntu 22.04 LTS hoặc 24.04 LTS (x86_64).
- **Cấu hình tối thiểu**: 1 vCPU, 2GB RAM (khuyến nghị 2 vCPU, 4GB RAM), 25GB SSD.
- **Nhà cung cấp gợi ý**: Hetzner, DigitalOcean, Linode, Vultr hoặc VPS Việt Nam (Vietnix, BKHOST, Viettel Cloud) chi phí từ 80k - 150k VNĐ/tháng.

---

### BƯỚC 1: Cài đặt Docker & Docker Compose trên Ubuntu VPS
Kết nối SSH vào VPS bằng Terminal / PowerShell:
```bash
ssh root@<IP_VPS_CUA_BAN>
```

Cập nhật hệ thống và cài đặt Docker Engine:
```bash
sudo apt-get update && sudo apt-get upgrade -y
sudo apt-get install -y ca-certificates curl gnupg lsb-release git

# Cài đặt Docker và Docker Compose Plugin chính thức
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Kiểm tra phiên bản Docker
docker --version
docker compose version
```

---

### BƯỚC 2: Tải Mã Nguồn & Cấu Hình Biến Môi Trường
1. Clone repository về VPS:
   ```bash
   git clone https://github.com/luongduyenhop/popworld.git
   cd popworld
   ```

2. Tạo file cấu hình bí mật `.env` từ `.env.example`:
   ```bash
   cp .env.example .env
   nano .env
   ```

3. Điền các giá trị Production thực tế trong file `.env`:
   ```dotenv
   PORT=8080
   SPRING_PROFILES_ACTIVE=prod
   
   # Database MySQL
   DB_HOST=mysql
   DB_PORT=3306
   DB_NAME=popworld_db
   DB_USERNAME=root
   DB_PASSWORD=MatKhauDatabaseManh123@!
   JPA_DDL_AUTO=update
   
   # Cổng thanh toán SePay
   SEPAY_WEBHOOK_API_KEY=sepay_secret_token_cua_ban
   SEPAY_WEBHOOK_SECRET_KEY=sepay_secret_token_cua_ban
   SEPAY_BANK_CODE=MBBank
   SEPAY_ACCOUNT_NUMBER=0355416208
   SEPAY_ACCOUNT_NAME=POPWORLD OFFICIAL STORE
   
   # Cloudinary Media
   CLOUDINARY_CLOUD_NAME=ten_cloud_cua_ban
   CLOUDINARY_API_KEY=api_key_cua_ban
   CLOUDINARY_API_SECRET=api_secret_cua_ban
   CLOUDINARY_FOLDER=popworld/products
   
   # Bảo mật
   REMEMBER_ME_KEY=chuoi_ngau_nhien_dai_it_nhat_32_ky_tu_bao_mat_2026
   SESSION_COOKIE_SECURE=true
   APP_INIT_DEMO_DATA=false
   ```
   *(Nhấn `Ctrl + O` -> `Enter` để lưu, `Ctrl + X` để thoát `nano`)*.

---

### BƯỚC 3: Khởi Chạy Hệ Thống Bằng Docker Compose
Chạy toàn bộ cụm dịch vụ trên nền background:
```bash
docker compose up -d --build
```

Kiểm tra trạng thái các container đang chạy:
```bash
docker compose ps
```
*Kết quả mong đợi*: Cả `popworld-mysql` và `popworld-app` đều có trạng thái `Up (healthy)`.

Kiểm tra log khởi động của ứng dụng Spring Boot:
```bash
docker compose logs -f app
```
Khi nhìn thấy dòng log:
`Started PopworldApplication in X.XXX seconds (process running for X.XXX)`
nghĩa là ứng dụng đã chạy thành công 100%!

Kiểm tra endpoint sức khỏe nội bộ:
```bash
curl http://localhost:8080/api/health
# Trả về: {"status":"UP","service":"PopWorld Art Toy Platform",...}
```

---

### BƯỚC 4: Kích Hoạt Cloudflare Tunnel Đưa Website Lên Mạng (Có HTTPS & WAF)
Không cần mở cổng router hay firewall, Cloudflare Tunnel sẽ tạo đường truyền an toàn từ VPS ra mạng toàn cầu:

#### Cách 1: Chạy Quick Tunnel Demo (Dành cho báo cáo đồ án, không cần mua tên miền)
```bash
# Tải cloudflared trên Linux VPS
curl -L --output cloudflared.deb https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb
sudo dpkg -i cloudflared.deb

# Chạy Quick Tunnel
cloudflared tunnel --url http://localhost:8080
```
Màn hình sẽ hiển thị URL công khai (vd: `https://xyz-random.trycloudflare.com`). Bạn có thể dùng URL này truy cập website từ điện thoại hoặc máy tính bất kỳ!

#### Cách 2: Triển Khai Production Vĩnh Viễn Với Tên Miền Riêng (vd: `popworld.store`)
1. Thêm domain vào tài khoản [Cloudflare Dashboard](https://dash.cloudflare.com).
2. Vào **Zero Trust** -> **Networks** -> **Tunnels** -> **Create a Tunnel**.
3. Chọn hệ điều hành **Debian 64-bit**, copy câu lệnh do Cloudflare cung cấp và dán vào VPS:
   ```bash
   sudo cloudflared service install <TOKEN_DO_CLOUDFLARE_CAP>
   ```
4. Trên giao diện Cloudflare, cấu hình **Public Hostname**:
   - Subdomain: `popworld.store` (hoặc `www`)
   - Service: `HTTP` -> `localhost:8080`
5. Bấm **Save tunnel**. Tên miền của bạn sẽ có khóa xanh HTTPS tự động trong 30 giây!

---

### BƯỚC 5: Cấu Hình Webhook Trên Cổng SePay
1. Đăng nhập vào [https://my.sepay.vn](https://my.sepay.vn) -> Chọn mục **Tích hợp Webhook**.
2. Điền thông tin:
   - **Webhook URL**: `https://<TEN_MIEN_CUA_BAN>/api/payment/sepay/webhook`
   - **Authentication**: Bearer Token hoặc API Key.
   - **Token**: Điền đúng giá trị `SEPAY_WEBHOOK_API_KEY` đã đặt trong file `.env`.
3. Bấm **Lưu cấu hình** và thực hiện tính năng **Gửi webhook thử nghiệm** để xác nhận kết nối thành công.

---

## 3. KỊCH BẢN B: TRIỂN KHAI LÊN PAAS CLOUD (RAILWAY HOẶC RENDER)

Nếu bạn không muốn tự quản trị máy chủ VPS:

1. **Railway.app**:
   - Đăng ký tài khoản Railway và liên kết với tài khoản GitHub.
   - Bấm **New Project** -> **Deploy from GitHub repo** -> Chọn repository `popworld`.
   - Bấm **Add Database** -> Chọn **MySQL**.
   - Vào mục **Variables** của service `popworld`, copy các biến môi trường từ file `.env.example` vào. Railway tự động cấp biến `MYSQL_URL`, bạn chỉ cần gán `DB_URL=${{MySQL.MYSQL_URL}}`.
   - Railway sẽ tự động đọc file `Dockerfile` của dự án để build và cấp phát domain HTTPS miễn phí.

---

## 4. QUẢN TRỊ VẬN HÀNH & BẢO TRÌ (Ops Maintenance)

### 1. Cập Nhật Mã Nguồn Khi Có Phiên Bản Mới
Khi bạn push commit mới lên GitHub:
```bash
cd popworld
git pull origin main
docker compose up -d --build
```
Docker sẽ tự động biên dịch lại container ứng dụng và khởi động lại trong vòng 30 giây mà không làm mất dữ liệu database.

### 2. Sao Lưu Cơ Sở Dữ Liệu MySQL (Backup)
Tạo bản sao lưu định kỳ ra file SQL nén:
```bash
docker exec popworld-mysql mysqldump -u root -pMatKhauDatabaseManh123@! popworld_db | gzip > backup_$(date +%Y%m%d_%H%M%S).sql.gz
```

### 3. Khôi Phục Cơ Sở Dữ Liệu (Restore)
```bash
gunzip < backup_20261010_000000.sql.gz | docker exec -i popworld-mysql mysql -u root -pMatKhauDatabaseManh123@! popworld_db
```

### 4. Xem Nhật Ký Log Hệ Thống
```bash
# Xem 100 dòng log gần nhất của ứng dụng
docker compose logs --tail=100 -f app

# Xem log của database MySQL
docker compose logs --tail=50 -f mysql
```
