# Hướng Dẫn Triển Khai Hệ Thống PopWorld Trên Máy Chủ VPS Ubuntu

Tài liệu này cung cấp quy trình hướng dẫn từng bước (step-by-step) để đóng gói, triển khai và vận hành nền tảng thương mại điện tử Art Toy **PopWorld** trên máy chủ ảo (VPS) chạy hệ điều hành Ubuntu Linux với Docker Compose, Nginx Reverse Proxy và chứng chỉ SSL/TLS miễn phí từ Let's Encrypt Certbot.

---

## 1. Yêu Cầu Tiên Quyết & Cấu Hình Phần Cứng (Prerequisites)

### 1.1. Cấu hình phần cứng tối thiểu đề xuất
- **Hệ điều hành**: Ubuntu 22.04 LTS hoặc Ubuntu 24.04 LTS (64-bit).
- **CPU**: Tối thiểu 2 vCPU (khuyến nghị để xử lý JVM và MySQL mượt mà).
- **RAM**: Tối thiểu 4 GB RAM (Spring Boot JVM chiếm khoảng 1-1.5 GB, MySQL 8 chiếm khoảng 1-1.5 GB, Nginx & OS chiếm 500 MB - 1 GB). *Nếu VPS có 2 GB RAM, bắt buộc phải tạo Swap dung lượng tối thiểu 2-4 GB*.
- **Ổ cứng (Disk)**: Tối thiểu 25 GB SSD/NVMe trống.
- **Quyền hạn**: Người dùng có quyền `sudo` hoặc tài khoản `root`.

### 1.2. Cấu hình Tên miền (Domain & DNS)
- Bạn cần sở hữu 1 tên miền (ví dụ: `popworld.vn` hoặc `yourdomain.com`).
- Trỏ các bản ghi DNS sau về địa chỉ Public IP của VPS:
  - Bản ghi `A`: `@` -> `<IP_CUA_VPS>`
  - Bản ghi `A`: `www` -> `<IP_CUA_VPS>`
- Kiểm tra tính phân giải DNS trước khi tiến hành:
  ```bash
  ping yourdomain.com
  ```

---

## 2. Củng Cố Bảo Mật Máy Chủ & Tường Lửa UFW (Server Hardening)

Trước khi cài đặt ứng dụng, cần thiết lập bảo mật cơ bản cho VPS để ngăn chặn quét cổng và tấn công brute-force.

### 2.1. Cập nhật hệ thống
```bash
sudo apt update && sudo apt upgrade -y
```

### 2.2. Thiết lập tường lửa UFW (Uncomplicated Firewall)
Chỉ mở các cổng cần thiết cho web và quản trị từ xa qua SSH:
```bash
# Cho phép kết nối SSH (đảm bảo không bị mất quyền truy cập)
sudo ufw allow OpenSSH

# Cho phép cổng HTTP và HTTPS công khai cho Nginx
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Đảm bảo các cổng nội bộ như MySQL (3306) và Spring Boot (8080) KHÔNG được public ra ngoài
# Kích hoạt UFW
sudo ufw enable
sudo ufw status verbose
```

*Lưu ý bảo mật về Docker và UFW*: Trên hệ điều hành Linux (Ubuntu), Docker tự động thiết lập các quy tắc NAT trong bảng `iptables PREROUTING`, các quy tắc này được ưu tiên xử lý trước tường lửa UFW. Nếu dịch vụ Docker publish cổng ra `0.0.0.0`, kẻ tấn công bên ngoài có thể truy cập trực tiếp vượt qua UFW. Do đó, trong `docker-compose.yml`, các cổng `8080` (Spring Boot) và `3306` (MySQL) bắt buộc phải ràng buộc vào loopback `127.0.0.1` (`127.0.0.1:8080:8080` và `127.0.0.1:3306:3306`), đảm bảo chỉ có Nginx chạy cục bộ trên máy chủ mới có thể kết nối tới ứng dụng.

### 2.3. Tạo Swap File (Khuyến nghị cho VPS 2GB - 4GB RAM)
```bash
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

---

## 3. Cài Đặt Docker & Docker Compose

Sử dụng script cài đặt chính thức từ Docker:

```bash
# Tải và chạy script cài đặt Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Thêm người dùng hiện tại vào nhóm docker (để chạy docker không cần sudo)
sudo usermod -aG docker $USER
newgrp docker

# Kích hoạt Docker service khởi động cùng hệ thống
sudo systemctl enable docker
sudo systemctl start docker

# Kiểm tra phiên bản cài đặt thành công
docker --version
docker compose version
```

---

## 4. Cài Đặt Nginx & Cấp Chứng Chỉ SSL Let's Encrypt Certbot

Hệ thống sử dụng Nginx làm Reverse Proxy đứng trước Spring Boot Docker Container để đảm nhiệm:
- Chấm dứt kết nối SSL/TLS (SSL Termination) và chuyển hướng HTTP sang HTTPS.
- Chuyển tiếp tiêu đề IP thực (`X-Forwarded-For`, `X-Forwarded-Proto`, `X-Real-IP`).
- Giới hạn tải lên tới 25MB (`client_max_body_size 25M;`) cho hình ảnh sản phẩm.
- Nén Gzip tăng tốc độ tải tài nguyên tĩnh.

### 4.1. Cài đặt Nginx và Certbot
```bash
sudo apt install -y nginx certbot python3-certbot-nginx
sudo systemctl enable nginx
sudo systemctl start nginx
```

### 4.2. Khởi tạo thư mục xác thực ACME Challenge (phục vụ gia hạn định kỳ)
```bash
sudo mkdir -p /var/www/certbot
sudo chown -R www-data:www-data /var/www/certbot
```

### 4.3. Cấp chứng chỉ SSL/TLS tự động với Certbot (Chế độ Standalone - Giải quyết Deadlock)
> **Giải quyết Deadlock Khởi tạo Chứng chỉ**: Khi cài đặt lần đầu trên máy chủ mới, nếu dùng Certbot với webroot `/var/www/certbot` trong khi Nginx mặc định đang trỏ về `/var/www/html` thì Let's Encrypt sẽ trả về lỗi 404. Ngược lại, nếu kích hoạt trước cấu hình `popworld.conf` khi chưa có file chứng chỉ, lệnh `nginx -t` sẽ thất bại (`cannot load certificate`) và Nginx từ chối khởi động. Do đó, phương án chuẩn xác nhất là tạm dừng Nginx và chạy Certbot ở chế độ `--standalone` để tự mở cổng 80 cấp chứng chỉ:

```bash
# 1. Tạm dừng Nginx để giải phóng cổng 80 cho Certbot standalone
sudo systemctl stop nginx

# 2. Cấp chứng chỉ SSL/TLS tự động cho tên miền
sudo certbot certonly --standalone -d yourdomain.com -d www.yourdomain.com

# 3. Khởi động lại Nginx
sudo systemctl start nginx
```
*Lưu ý*: Thay `yourdomain.com` bằng tên miền thực tế. Sau khi hoàn tất, chứng chỉ sẽ được lưu tại:
- `/etc/letsencrypt/live/yourdomain.com/fullchain.pem`
- `/etc/letsencrypt/live/yourdomain.com/privkey.pem`

*(Ghi chú gia hạn*: Sau khi hoàn tất bước 4.4 bên dưới, các lần tự động gia hạn chứng chỉ định kỳ sau này sẽ hoạt động thông qua ACME webroot `/var/www/certbot` đã được định tuyến sẵn trong `popworld.conf`, không cần phải dừng Nginx nữa).*

### 4.4. Cấu hình Nginx cho PopWorld
1. Sao chép file cấu hình `nginx/popworld.conf` vào Nginx:
   ```bash
   sudo cp /opt/popworld/nginx/popworld.conf /etc/nginx/sites-available/popworld
   ```
2. Thay thế `yourdomain.com` bằng tên miền thực tế của bạn trong file:
   ```bash
   sudo sed -i 's/yourdomain.com/popworld.vn/g' /etc/nginx/sites-available/popworld
   ```
3. Kích hoạt Virtual Host và tắt cấu hình mặc định:
   ```bash
   sudo ln -sf /etc/nginx/sites-available/popworld /etc/nginx/sites-enabled/
   sudo rm -f /etc/nginx/sites-enabled/default
   ```
4. Kiểm tra cú pháp và nạp lại cấu hình:
   ```bash
   sudo nginx -t
   sudo systemctl reload nginx
   ```

---

## 5. Cài Đặt Ứng Dụng PopWorld & Cấu Hình `.env`

### 5.1. Tải mã nguồn về máy chủ
```bash
sudo mkdir -p /opt/popworld
sudo chown -R $USER:$USER /opt/popworld
cd /opt/popworld
git clone <URL_REPO_GIT> .
```

### 5.2. Tạo file cấu hình biến môi trường `.env`
Sao chép từ `.env.example` và điều chỉnh các thông số bảo mật:
```bash
cp .env.example .env
nano .env
```

Nội dung file `.env` khuyến nghị cho môi trường Production:
```dotenv
# ==============================================================================
# POPWORLD PRODUCTION ENVIRONMENT CONFIGURATION (.env)
# ==============================================================================

# 1. Cổng lắng nghe và Profile
PORT=8080
SPRING_PROFILES_ACTIVE=prod

# 2. Cổng thanh toán tự động SePay VietQR (https://my.sepay.vn)
SEPAY_WEBHOOK_API_KEY=your_production_webhook_api_key_2026
SEPAY_WEBHOOK_SECRET_KEY=your_production_webhook_secret_key_2026
SEPAY_BANK_CODE=MBBank
SEPAY_ACCOUNT_NUMBER=0355416208
SEPAY_ACCOUNT_NAME=POPWORLD OFFICIAL STORE

# 3. Lưu trữ đám mây Cloudinary (Nếu để trống, ảnh lưu tại thư mục cục bộ uploads/)
CLOUDINARY_CLOUD_NAME=your_cloudinary_cloud_name
CLOUDINARY_API_KEY=your_cloudinary_api_key
CLOUDINARY_API_SECRET=your_cloudinary_api_secret
CLOUDINARY_FOLDER=popworld/products

# 4. Cơ sở dữ liệu MySQL
DB_HOST=mysql
DB_PORT=3306
DB_NAME=popworld_db
DB_USERNAME=root
DB_PASSWORD=ChonMatKhauMySQLPhucTap_2026!
JPA_DDL_AUTO=update

# 5. Bảo mật Phiên làm việc (Session & Cookie)
REMEMBER_ME_KEY=TaoChuoiNgauNhienDaiItNhat32KyTu_2026!
SESSION_COOKIE_SECURE=true

# 6. Dữ liệu mẫu (true: nạp catalog PopMart nếu chạy lần đầu; false: bảo lưu DB hiện có)
APP_INIT_DEMO_DATA=false
```

### 5.3. Khởi chạy cụm Container với Docker Compose

Cấu hình `docker-compose.yml` tích hợp các cơ chế an toàn vận hành:
1. **Lưu trữ dữ liệu bền vững (Data Persistence)**:
   - `mysql_data` (`popworld_mysql_data`): Lưu trữ toàn bộ dữ liệu CSDL MySQL tại `/var/lib/mysql`.
   - `app_uploads` (`popworld_app_uploads`): Ánh xạ vào thư mục `/app/uploads` trong container Spring Boot. Điều này đảm bảo toàn bộ hình ảnh sản phẩm, avatar người dùng tải lên cục bộ được bảo toàn trọn vẹn, không bị mất khi container ứng dụng được rebuild hoặc cập nhật phiên bản mới.
2. **Cô lập mạng & Bảo mật cổng (Port Isolation)**:
   - `127.0.0.1:${PORT:-8080}:8080`: Chỉ cho phép Nginx cục bộ kết nối vào ứng dụng, ngăn chặn hoàn toàn việc bypass tường lửa UFW qua iptables NAT của Docker.
   - `127.0.0.1:${DB_EXTERNAL_PORT:-3306}:3306`: Cổng CSDL chỉ lắng nghe trên localhost để quản trị nội bộ nếu cần, không lộ ra Internet.

Khởi chạy ứng dụng:
```bash
# Build Docker image và khởi chạy ngầm
docker compose up -d --build

# Theo dõi tiến trình khởi động
docker compose logs -f app
```

Sau khoảng 20-30 giây, Spring Boot sẽ khởi chạy hoàn tất và báo:
`Started PopworldApplication in X.XXX seconds`

---

## 6. Danh Mục Kiểm Tra Tự Động & Nghiệm Thu Hệ Thống (Verification Checklist)

Sau khi triển khai, thực hiện các bước sau để xác nhận hệ thống vận hành trơn tru:

### 6.1. Kiểm tra trạng thái Container
```bash
docker compose ps
```
*Kết quả kỳ vọng*:
- `popworld-mysql`: Status `Up ... (healthy)`
- `popworld-app`: Status `Up ... (healthy)`

### 6.2. Kiểm tra Healthcheck Probe Endpoint
Chạy lệnh curl kiểm tra qua Nginx:
```bash
curl -i https://yourdomain.com/api/health
```
*Kết quả phản hồi kỳ vọng*:
```json
HTTP/2 200
content-type: application/json

{"status":"UP","app":"popworld","timestamp":"...","service":"PopWorld Art Toy Platform","version":"0.0.1-SNAPSHOT"}
```

### 6.3. Kiểm thử Đăng Nhập Tài Khoản Mẫu (Demo Credentials)
Hệ thống tích hợp sẵn các tài khoản mẫu phục vụ chấm điểm và trải nghiệm:
1. **Quản trị viên (Admin)**:
   - URL: `https://yourdomain.com/login`
   - Email: `admin@popworld.com` | Mật khẩu: `admin123`
   - *Ghi chú*: Cơ chế 2FA TOTP đã được tự động tắt (`totpEnabled = false`) trên tài khoản demo này, cho phép đăng nhập trực tiếp vào trang `/admin/dashboard` mà không bị kẹt mã OTP.
2. **Khách hàng tiêu chuẩn**:
   - Email: `user@popworld.com` | Mật khẩu: `admin123`
   - Đã có sẵn 2 địa chỉ giao hàng và giỏ hàng mẫu.
3. **VIP Collector**:
   - Email: `collector@popworld.com` | Mật khẩu: `admin123`
   - Hạng VIP, 1500 điểm tích lũy, 3 mã giảm giá (`POPWELCOME`, `FREESHIP`, `MEGAPOP500`).

### 6.4. Kiểm thử Luồng Thanh Toán QR & Chế Độ Mô Phỏng Demo
1. Thêm 1 sản phẩm Blind Box vào giỏ hàng hoặc chọn bóc hộp tại `/popnow`.
2. Đến trang thanh toán chuyển khoản SePay: `https://yourdomain.com/checkout/payment-sepay/{orderCode}`.
3. Xác nhận giao diện hiển thị mã VietQR động cùng mã đơn hàng `PW-XXXXX`.
4. Nhấp vào nút màu vàng **"Mô Phỏng Quét QR Thành Công (Demo Mode)"** trên giao diện:
   - Trạng thái đơn hàng sẽ ngay lập tức chuyển sang `PROCESSING` (Đã thanh toán).
   - Hệ thống tự động chuyển hướng sang màn hình khui seal PopNow `/popnow/reveal/{code}` hoặc trang thành công `/checkout/success/{code}`.
   - Luồng hoạt động hoàn toàn độc lập mà không cần chuyển khoản tiền thật.

---

## 7. Vận Hành, Sao Lưu Dữ Liệu & Cập Nhật Không Gián Đoạn (Maintenance & Backup)

### 7.1. Tự động sao lưu Cơ sở dữ liệu MySQL định kỳ (Automated Backup)
Tạo script sao lưu tự động tại `/opt/backup_popworld.sh` (sử dụng mật khẩu động từ file `.env` và biến môi trường `MYSQL_PWD` an toàn để không lộ thông tin xác thực trong danh sách tiến trình `ps aux` hay gây lỗi âm thầm khi quản trị viên đổi mật khẩu CSDL):
```bash
cat << 'EOF' | sudo tee /opt/backup_popworld.sh
#!/bin/bash
set -eo pipefail
BACKUP_DIR="/var/backups/popworld"
DATE=$(date +"%Y%m%d_%H%M%S")
mkdir -p "$BACKUP_DIR"

# Trích xuất mật khẩu CSDL động từ file .env
DB_PASS=$(grep -E '^DB_PASSWORD=' /opt/popworld/.env | cut -d '=' -f2-)
if [ -z "$DB_PASS" ]; then
    DB_PASS="admin123"
fi

# Dump dữ liệu MySQL từ container thông qua MYSQL_PWD bảo mật
docker exec -e MYSQL_PWD="$DB_PASS" popworld-mysql mysqldump -u root popworld_db | gzip > "$BACKUP_DIR/popworld_db_$DATE.sql.gz"

# Chỉ giữ các bản sao lưu trong vòng 14 ngày gần nhất
find "$BACKUP_DIR" -type f -name "*.sql.gz" -mtime +14 -delete
EOF

sudo chmod +x /opt/backup_popworld.sh
```

Thêm cron job chạy sao lưu lúc 02:00 sáng mỗi ngày:
```bash
(crontab -l 2>/dev/null; echo "0 2 * * * /opt/backup_popworld.sh > /dev/null 2>&1") | crontab -
```

### 7.2. Tự động gia hạn chứng chỉ SSL Certbot
Certbot tự động cài đặt timer gia hạn. Bạn có thể kiểm tra thử:
```bash
sudo certbot renew --dry-run
```

### 7.3. Quy trình Cập nhật Phiên bản Mới (Zero-Downtime Rolling Update)
Khi có phiên bản mã nguồn mới trên Git:
```bash
cd /opt/popworld

# 1. Kéo mã nguồn mới nhất
git pull origin main

# 2. Rebuild và thay thế container mà không làm gián đoạn MySQL
docker compose up -d --build --no-deps app

# 3. Kiểm tra container mới đã healthy
docker compose ps

# 4. Dọn dẹp các Docker image cũ không còn sử dụng
docker image prune -f
```

---

## 8. Xử Lý Sự Cố Thường Gặp (Troubleshooting)

| Hiện tượng | Nguyên nhân | Biện pháp xử lý |
|---|---|---|
| Lỗi 502 Bad Gateway trên Nginx | Spring Boot container chưa khởi động xong hoặc bị crash | Kiểm tra logs: `docker compose logs -f app`. Đợi 20s để JVM hoàn tất nạp context. |
| Container báo `unhealthy` | Endpoint `/api/health` trả về lỗi hoặc timeout | Kiểm tra log curl nội bộ: `docker exec -it popworld-app curl -v http://localhost:8080/api/health`. |
| Mất phiên đăng nhập sau mỗi click | `SESSION_COOKIE_SECURE=true` nhưng truy cập qua HTTP thường | Nếu chưa cấu hình HTTPS, tạm thời đổi `SESSION_COOKIE_SECURE=false` trong `.env` rồi restart container. |
| Không tải được ảnh lớn (>1MB) | Vượt quá giới hạn mặc định của Nginx | Đảm bảo file cấu hình Nginx có chỉ thị `client_max_body_size 25M;`. |
| Rate limit 429 quá nhanh hoặc bị bypass | Nginx chưa chuyển tiếp hoặc làm sạch `X-Forwarded-For` đúng cách | Đảm bảo file `popworld.conf` có chỉ thị `proxy_set_header X-Forwarded-For $remote_addr;` để chuyển tiếp chính xác IP máy khách thực tế. |
