# AuraChat Backend

Backend của hệ thống **Aura Chat** — nền tảng giao tiếp thời gian thực xây dựng bằng **Spring Boot 3.2.4 + JDK 21**.

## Tech Stack

| Thành phần | Công nghệ |
|---|---|
| Runtime | Java 21 (Virtual Threads) |
| Framework | Spring Boot 3.2.4 |
| Bảo mật | Spring Security + JWT + OAuth2 |
| Database | MongoDB 7 |
| Cache / Pub-Sub | Redis 7 |
| Real-time | WebSocket (STOMP) |
| Media | ImageKit.io |
| Container | Docker + Docker Compose |

## Cấu trúc thư mục

```
src/main/java/com/aurachat/
├── config/                  # JWT, Security, WebSocket config
├── module/
│   ├── auth/
│   │   ├── controller/      # AuthController
│   │   ├── entity/          # User, RefreshToken
│   │   ├── repository/      # UserRepository, RefreshTokenRepository
│   │   ├── service/         # AuthService, ForgotPasswordService
│   │   ├── handler/         # OAuth2SuccessHandler
│   │   └── dto/             # Request/Response DTOs
│   ├── message/             # Nhắn tin real-time
│   ├── presence/            # Trạng thái online/offline
│   ├── call/                # Cuộc gọi video/audio (WebRTC signaling)
│   └── friend/              # Tìm kiếm & kết bạn
```

---

## Yêu cầu

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) >= 24
- Docker Compose >= 2.20

---

## 1. Tạo file `.env`

Sao chép file mẫu rồi điền thông tin thực:

```bash
cp .env.example .env
```

Mở `.env` và điền các giá trị:

```dotenv
# ── Database (giữ nguyên nếu dùng Docker) ────────────────────────────────────
MONGODB_URI=mongodb://mongo:27017/aurachat
REDIS_HOST=redis
REDIS_PORT=6379

# ── JWT ───────────────────────────────────────────────────────────────────────
# Tạo chuỗi ngẫu nhiên >= 32 ký tự
# Windows PowerShell: [Convert]::ToBase64String((1..32 | % { [byte](Get-Random -Max 256) }))
# Linux/Mac:          openssl rand -base64 32
JWT_SECRET=thay-bang-chuoi-ngau-nhien-dai-hon-32-ky-tu

# ── OAuth2 - Google ───────────────────────────────────────────────────────────
# 1. Vào https://console.cloud.google.com
# 2. APIs & Services → Credentials → Create OAuth 2.0 Client ID
# 3. Authorized redirect URIs: http://localhost:8080/login/oauth2/code/google
GOOGLE_CLIENT_ID=xxxx.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=xxxx

# ── OAuth2 - Facebook ─────────────────────────────────────────────────────────
# 1. Vào https://developers.facebook.com → My Apps → Create App
# 2. Facebook Login → Settings → Valid OAuth Redirect URIs:
#    http://localhost:8080/login/oauth2/code/facebook
FACEBOOK_CLIENT_ID=xxxx
FACEBOOK_CLIENT_SECRET=xxxx

# ── Email (Forgot Password OTP) ───────────────────────────────────────────────
# Dùng Gmail App Password:
# 1. Bật 2FA tại myaccount.google.com/security
# 2. Tạo App Password tại myaccount.google.com/apppasswords
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=xxxx-xxxx-xxxx-xxxx   # App Password 16 ký tự

# ── OAuth2 Redirect về Frontend ───────────────────────────────────────────────
OAUTH2_REDIRECT_URI=http://localhost:3000/oauth2/callback

# ── ImageKit ──────────────────────────────────────────────────────────────────
# Lấy tại: https://imagekit.io/dashboard → Developer Options
IMAGEKIT_URL_ENDPOINT=https://ik.imagekit.io/your_id
IMAGEKIT_PUBLIC_KEY=public_xxxx
IMAGEKIT_PRIVATE_KEY=private_xxxx
```

> **Lưu ý:** Nếu chỉ muốn test tính năng chat cơ bản, chỉ cần điền `JWT_SECRET`. Các biến OAuth2, Email, ImageKit có thể để trống và bổ sung sau.

---

## 2. Chạy bằng Docker Compose

```bash
# Build và khởi động toàn bộ (backend + MongoDB + Redis)
docker compose up --build

# Chạy nền (detached)
docker compose up --build -d

# Xem logs
docker compose logs -f backend

# Dừng
docker compose down

# Dừng và xóa toàn bộ data (volumes)
docker compose down -v
```

Sau khi khởi động thành công:

| Service | URL |
|---|---|
| Backend API | http://localhost:8080 |
| MongoDB | localhost:27017 |
| Redis | localhost:6379 |

---

## 3. Chạy local (không Docker)

Yêu cầu: JDK 21, Maven 3.9+, MongoDB và Redis đang chạy local.

```bash
# Đổi MONGODB_URI và REDIS_HOST trong .env về localhost
MONGODB_URI=mongodb://localhost:27017/aurachat
REDIS_HOST=localhost

# Chạy
./mvnw spring-boot:run
```

---

## 4. API Endpoints chính

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/auth/register` | Đăng ký tài khoản |
| POST | `/api/auth/login` | Đăng nhập |
| POST | `/api/auth/refresh` | Làm mới access token |
| GET | `/api/auth/me` | Thông tin user hiện tại |
| POST | `/api/auth/forgot-password` | Gửi OTP qua email |
| POST | `/api/auth/reset-password` | Đặt lại mật khẩu bằng OTP |
| GET | `/oauth2/authorization/google` | Đăng nhập Google |
| GET | `/oauth2/authorization/facebook` | Đăng nhập Facebook |
| WebSocket | `ws://localhost:8080/ws` | Kết nối real-time |

---

## Thành viên thực hiện

- Huỳnh Linh Hoài
- Phan Văn Hoàng

Giảng viên hướng dẫn: **Lê Phi Hùng**
