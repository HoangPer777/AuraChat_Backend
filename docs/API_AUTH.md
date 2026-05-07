# AuraChat Authentication API Documentation

## Table of Contents
- [Overview](#overview)
- [Base URL](#base-url)
- [Response Format](#response-format)
- [Error Codes](#error-codes)
- [Authentication Endpoints](#authentication-endpoints)
  - [Register](#1-register)
  - [Login](#2-login)
  - [Firebase Login](#3-firebase-login-google--facebook)
  - [Refresh Token](#4-refresh-token)
  - [Logout](#5-logout)
- [User Profile Endpoints](#user-profile-endpoints)
  - [Get Profile](#6-get-profile)
  - [Update Profile](#7-update-profile)
  - [Upload Avatar](#8-upload-avatar)
- [Password Management](#password-management)
  - [Forgot Password](#9-forgot-password)
  - [Reset Password](#10-reset-password)

---

## Overview

AuraChat Authentication API cung cấp các endpoint để quản lý authentication và user profile. API hỗ trợ:
- ✅ Email/Password authentication
- ✅ Firebase authentication (Google, Facebook)
- ✅ JWT token-based authentication
- ✅ Refresh token rotation
- ✅ Password reset via OTP
- ✅ User profile management
- ✅ Avatar upload

---

## Base URL

```
http://localhost:8080
```

**Production:** Thay đổi theo domain của bạn

---

## Response Format

### Success Response

```json
{
  "success": true,
  "message": "Operation successful message",
  "data": {
    // Response data here
  },
  "timestamp": "2024-12-20T10:00:00Z"
}
```

### Error Response

```json
{
  "success": false,
  "message": "Error message description",
  "errorCode": "ERROR_CODE",
  "errorId": "ERR_20241220_100000_ABC123",
  "details": {
    // Additional error details (optional)
  },
  "timestamp": "2024-12-20T10:00:00Z"
}
```

---

## Error Codes

### Authentication Errors (AUTH_xxx)

| Code | Message | HTTP Status | Description |
|------|---------|-------------|-------------|
| `AUTH_001` | Invalid username or password | 401 | Sai email hoặc password |
| `AUTH_002` | Authentication token has expired | 401 | Access token đã hết hạn |
| `AUTH_003` | Invalid authentication token | 401 | Token không hợp lệ |
| `AUTH_004` | Account is locked | 401 | Tài khoản bị khóa |
| `AUTH_005` | Access denied | 403 | Không có quyền truy cập |
| `AUTH_006` | Authentication failed | 401 | Xác thực thất bại |

### Validation Errors (VAL_xxx)

| Code | Message | HTTP Status | Description |
|------|---------|-------------|-------------|
| `VAL_001` | Validation failed | 400 | Lỗi validation chung |
| `VAL_002` | Required field is missing | 400 | Thiếu trường bắt buộc |
| `VAL_003` | Invalid field format | 400 | Format không hợp lệ |
| `VAL_004` | Field value exceeds maximum length | 400 | Giá trị quá dài |

### User Errors (USER_xxx)

| Code | Message | HTTP Status | Description |
|------|---------|-------------|-------------|
| `USER_001` | Email address already exists | 422 | Email đã tồn tại |
| `USER_002` | User not found | 404 | Không tìm thấy user |
| `USER_003` | Failed to update user profile | 422 | Cập nhật profile thất bại |

### Media Errors (MEDIA_xxx)

| Code | Message | HTTP Status | Description |
|------|---------|-------------|-------------|
| `MEDIA_001` | Failed to upload media file | 422 | Upload thất bại |
| `MEDIA_002` | Invalid media file type | 400 | File type không hợp lệ |
| `MEDIA_003` | Media file size exceeds limit | 400 | File quá lớn (>5MB) |

### System Errors (SYS_xxx)

| Code | Message | HTTP Status | Description |
|------|---------|-------------|-------------|
| `SYS_001` | Internal system error | 500 | Lỗi hệ thống |
| `SYS_002` | Database operation failed | 500 | Lỗi database |
| `SYS_003` | External service unavailable | 503 | Service bên ngoài lỗi |
| `SYS_004` | System configuration error | 500 | Lỗi cấu hình |

---

## Authentication Endpoints

### 1. Register

Đăng ký tài khoản mới với email và password.

**Endpoint:** `POST /api/auth/register`

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "displayName": "John Doe"
}
```

**Validation Rules:**
- `email`: Required, must be valid email format
- `password`: Required, minimum 8 characters, maximum 100 characters
- `displayName`: Required, minimum 2 characters, maximum 100 characters

**Success Response:** `200 OK`
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "tokenType": "Bearer",
    "user": {
      "id": "65a8c42e1234567890",
      "email": "user@example.com",
      "displayName": "John Doe",
      "avatarUrl": null,
      "bio": null
    }
  },
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**Error Responses:**

**400 Bad Request** - Validation error
```json
{
  "success": false,
  "message": "Validation failed",
  "errorCode": "VAL_001",
  "errorId": "ERR_20241220_100000_ABC123",
  "details": {
    "email": "Email format is invalid",
    "password": "Password must be at least 8 characters"
  },
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**422 Unprocessable Entity** - Email already exists
```json
{
  "success": false,
  "message": "Email address already exists",
  "errorCode": "USER_001",
  "errorId": "ERR_20241220_100000_DEF456",
  "details": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!",
    "displayName": "John Doe"
  }'
```

---

### 2. Login

Đăng nhập với email và password.

**Endpoint:** `POST /api/auth/login`

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Validation Rules:**
- `email`: Required, must be valid email format
- `password`: Required

**Success Response:** `200 OK`
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "tokenType": "Bearer",
    "user": {
      "id": "65a8c42e1234567890",
      "email": "user@example.com",
      "displayName": "John Doe",
      "avatarUrl": "https://example.com/avatar.jpg",
      "bio": "Hello, I'm John!"
    }
  },
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**Error Responses:**

**401 Unauthorized** - Invalid credentials
```json
{
  "success": false,
  "message": "Invalid username or password",
  "errorCode": "AUTH_001",
  "errorId": "ERR_20241220_100000_GHI789",
  "details": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!"
  }'
```

---

### 3. Firebase Login (Google / Facebook)

Đăng nhập bằng Firebase ID Token (từ Google hoặc Facebook).

**Endpoint:** `POST /api/auth/firebase/login`

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEyMzQ1Njc4OTAiLCJ0eXAiOiJKV1QifQ..."
}
```

**Validation Rules:**
- `idToken`: Required, must be valid Firebase ID Token

**Success Response:** `200 OK`
```json
{
  "success": true,
  "message": "Firebase login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "tokenType": "Bearer",
    "user": {
      "id": "65a8c42e1234567890",
      "email": "user@gmail.com",
      "displayName": "John Doe",
      "avatarUrl": "https://lh3.googleusercontent.com/a/default-user=s96-c",
      "bio": null
    }
  },
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**Error Responses:**

**401 Unauthorized** - Invalid Firebase token
```json
{
  "success": false,
  "message": "Invalid Firebase ID token: Token verification failed",
  "errorCode": "AUTH_003",
  "errorId": "ERR_20241220_100000_JKL012",
  "details": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/auth/firebase/login \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEyMzQ1Njc4OTAiLCJ0eXAiOiJKV1QifQ..."
  }'
```

**Note:** Xem thêm tài liệu chi tiết về Firebase login tại [FIREBASE_LOGIN_API.md](./FIREBASE_LOGIN_API.md)

---

### 4. Refresh Token

Làm mới access token khi hết hạn.

**Endpoint:** `POST /api/auth/refresh`

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Validation Rules:**
- `refreshToken`: Required

**Success Response:** `200 OK`
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "660e8400-e29b-41d4-a716-446655440111",
    "tokenType": "Bearer",
    "user": {
      "id": "65a8c42e1234567890",
      "email": "user@example.com",
      "displayName": "John Doe",
      "avatarUrl": "https://example.com/avatar.jpg",
      "bio": "Hello, I'm John!"
    }
  },
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**Error Responses:**

**401 Unauthorized** - Invalid refresh token
```json
{
  "success": false,
  "message": "Invalid refresh token",
  "errorCode": "AUTH_003",
  "errorId": "ERR_20241220_100000_MNO345",
  "details": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**401 Unauthorized** - Expired refresh token
```json
{
  "success": false,
  "message": "Refresh token expired",
  "errorCode": "AUTH_002",
  "errorId": "ERR_20241220_100000_PQR678",
  "details": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

**Note:** 
- Refresh token rotation được áp dụng: token cũ sẽ bị xóa và token mới được tạo
- Refresh token có thời hạn 7 ngày
- Access token có thời hạn 1 giờ

---

### 5. Logout

Đăng xuất và xóa refresh token.

**Endpoint:** `POST /api/auth/logout`

**Headers:**
```
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Request Body:** None

**Success Response:** `200 OK`
```json
{
  "success": true,
  "message": "Logged out successfully",
  "data": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**Error Responses:**

**401 Unauthorized** - Invalid or expired token
```json
{
  "success": false,
  "message": "Authentication token has expired",
  "errorCode": "AUTH_002",
  "errorId": "ERR_20241220_100000_STU901",
  "details": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

---

## User Profile Endpoints

### 6. Get Profile

Lấy thông tin profile của user hiện tại.

**Endpoint:** `GET /api/auth/me`

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Request Body:** None

**Success Response:** `200 OK`
```json
{
  "success": true,
  "message": "User profile retrieved successfully",
  "data": {
    "id": "65a8c42e1234567890",
    "email": "user@example.com",
    "displayName": "John Doe",
    "avatarUrl": "https://example.com/avatar.jpg",
    "bio": "Hello, I'm John!"
  },
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**Error Responses:**

**401 Unauthorized** - Invalid or expired token
```json
{
  "success": false,
  "message": "Authentication token has expired",
  "errorCode": "AUTH_002",
  "errorId": "ERR_20241220_100000_VWX234",
  "details": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**404 Not Found** - User not found
```json
{
  "success": false,
  "message": "User not found",
  "errorCode": "USER_002",
  "errorId": "ERR_20241220_100000_YZA567",
  "details": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**cURL Example:**
```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 7. Update Profile

Cập nhật thông tin profile (displayName, bio).

**Endpoint:** `PATCH /api/auth/me`

**Headers:**
```
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Request Body:**
```json
{
  "displayName": "John Smith",
  "bio": "Software Engineer | Tech Enthusiast"
}
```

**Validation Rules:**
- `displayName`: Optional, minimum 2 characters, maximum 100 characters
- `bio`: Optional, maximum 500 characters

**Success Response:** `200 OK`
```json
{
  "success": true,
  "message": "Profile updated successfully",
  "data": {
    "id": "65a8c42e1234567890",
    "email": "user@example.com",
    "displayName": "John Smith",
    "avatarUrl": "https://example.com/avatar.jpg",
    "bio": "Software Engineer | Tech Enthusiast"
  },
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**Error Responses:**

**400 Bad Request** - Validation error
```json
{
  "success": false,
  "message": "Validation failed",
  "errorCode": "VAL_001",
  "errorId": "ERR_20241220_100000_BCD890",
  "details": {
    "displayName": "Display name must be at least 2 characters",
    "bio": "Bio exceeds maximum length of 500 characters"
  },
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**401 Unauthorized** - Invalid or expired token
```json
{
  "success": false,
  "message": "Authentication token has expired",
  "errorCode": "AUTH_002",
  "errorId": "ERR_20241220_100000_EFG123",
  "details": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**cURL Example:**
```bash
curl -X PATCH http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "displayName": "John Smith",
    "bio": "Software Engineer | Tech Enthusiast"
  }'
```

---

### 8. Upload Avatar

Upload ảnh đại diện (avatar) cho user.

**Endpoint:** `POST /api/auth/me/avatar`

**Headers:**
```
Authorization: Bearer <accessToken>
Content-Type: multipart/form-data
```

**Request Body:**
```
file: <image-file>
```

**Validation Rules:**
- File type: JPEG, PNG, GIF, WebP only
- Maximum file size: 5MB
- Field name must be `file`

**Success Response:** `200 OK`
```json
{
  "success": true,
  "message": "Avatar uploaded successfully",
  "data": "https://ik.imagekit.io/aurachat/avatar/avatar_65a8c42e1234567890_abc123.jpg",
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**Error Responses:**

**400 Bad Request** - Invalid file type
```json
{
  "success": false,
  "message": "Invalid media file type",
  "errorCode": "MEDIA_002",
  "errorId": "ERR_20241220_100000_HIJ456",
  "details": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**400 Bad Request** - File too large
```json
{
  "success": false,
  "message": "Media file size exceeds limit",
  "errorCode": "MEDIA_003",
  "errorId": "ERR_20241220_100000_KLM789",
  "details": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**422 Unprocessable Entity** - Upload failed
```json
{
  "success": false,
  "message": "Failed to upload media file",
  "errorCode": "MEDIA_001",
  "errorId": "ERR_20241220_100000_NOP012",
  "details": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/auth/me/avatar \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -F "file=@/path/to/avatar.jpg"
```

**Note:** 
- Avatar được lưu trữ trên ImageKit
- URL trả về là URL công khai có thể truy cập trực tiếp
- Avatar cũ sẽ bị ghi đè bởi avatar mới

---

## Password Management

### 9. Forgot Password

Gửi OTP qua email để reset password.

**Endpoint:** `POST /api/auth/forgot-password`

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Validation Rules:**
- `email`: Required, must be valid email format

**Success Response:** `200 OK`
```json
{
  "success": true,
  "message": "If that email exists, an OTP has been sent",
  "data": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**Error Responses:**

**400 Bad Request** - Invalid email format
```json
{
  "success": false,
  "message": "Invalid field format",
  "errorCode": "VAL_003",
  "errorId": "ERR_20241220_100000_QRS345",
  "details": {
    "email": "Email format is invalid"
  },
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com"
  }'
```

**Note:**
- OTP có thời hạn 10 phút
- OTP được gửi qua email
- Response luôn trả về success để tránh email enumeration attack

---

### 10. Reset Password

Reset password bằng OTP.

**Endpoint:** `POST /api/auth/reset-password`

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "otp": "123456",
  "newPassword": "NewSecurePass123!"
}
```

**Validation Rules:**
- `email`: Required, must be valid email format
- `otp`: Required, 6 digits
- `newPassword`: Required, minimum 8 characters

**Success Response:** `200 OK`
```json
{
  "success": true,
  "message": "Password reset successfully",
  "data": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**Error Responses:**

**401 Unauthorized** - Invalid OTP
```json
{
  "success": false,
  "message": "Invalid or expired OTP",
  "errorCode": "AUTH_003",
  "errorId": "ERR_20241220_100000_TUV678",
  "details": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**400 Bad Request** - Validation error
```json
{
  "success": false,
  "message": "Validation failed",
  "errorCode": "VAL_001",
  "errorId": "ERR_20241220_100000_WXY901",
  "details": {
    "newPassword": "Password must be at least 8 characters"
  },
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "otp": "123456",
    "newPassword": "NewSecurePass123!"
  }'
```

---

## Token Management

### Access Token
- **Lifetime:** 1 hour
- **Usage:** Include in `Authorization: Bearer <token>` header
- **Refresh:** Use refresh token endpoint when expired

### Refresh Token
- **Lifetime:** 7 days
- **Usage:** Send to `/api/auth/refresh` to get new access token
- **Rotation:** Old token is deleted when refreshed
- **Storage:** Store securely (httpOnly cookie recommended)

---

## Common HTTP Status Codes

| Status Code | Meaning | When It Occurs |
|-------------|---------|----------------|
| 200 | OK | Request successful |
| 400 | Bad Request | Validation error, invalid input |
| 401 | Unauthorized | Invalid/expired token, wrong credentials |
| 403 | Forbidden | No permission to access resource |
| 404 | Not Found | Resource not found |
| 422 | Unprocessable Entity | Business logic error |
| 500 | Internal Server Error | Server error |
| 503 | Service Unavailable | External service down |

---

## Security Best Practices

### For Frontend Developers

1. **Store tokens securely:**
   - Use httpOnly cookies in production
   - Avoid localStorage if possible
   - Never expose tokens in URLs or logs

2. **Handle token expiration:**
   - Implement automatic token refresh
   - Handle 401 responses gracefully
   - Redirect to login when refresh fails

3. **HTTPS only:**
   - Always use HTTPS in production
   - Never send tokens over HTTP

4. **Validate input:**
   - Validate on frontend before sending
   - Don't rely only on backend validation

5. **Error handling:**
   - Display user-friendly error messages
   - Log errors for debugging
   - Don't expose sensitive information

### For Backend Developers

1. **Password security:**
   - Passwords are hashed with BCrypt (strength 10)
   - Never log passwords
   - Enforce strong password policies

2. **Token security:**
   - JWT tokens are signed with HS256
   - Refresh tokens are UUIDs stored in database
   - Tokens are validated on every request

3. **Rate limiting:**
   - Implement rate limiting for auth endpoints
   - Prevent brute force attacks

4. **Logging:**
   - Log all authentication attempts
   - Include error tracking IDs
   - Mask sensitive data in logs

---

## Testing

### Using Postman

1. **Import Collection:**
   - Create a new collection
   - Add all endpoints from this documentation

2. **Environment Variables:**
   ```
   BASE_URL = http://localhost:8080
   ACCESS_TOKEN = (set after login)
   REFRESH_TOKEN = (set after login)
   ```

3. **Test Flow:**
   ```
   1. Register → Save tokens
   2. Login → Save tokens
   3. Get Profile → Use access token
   4. Update Profile → Use access token
   5. Upload Avatar → Use access token
   6. Logout → Use access token
   ```

### Using cURL

See cURL examples in each endpoint section above.

### Using Test HTML Page

Access the test page at:
```
http://localhost:8080/test-firebase-login.html
```

---

## Troubleshooting

### Issue: "Invalid authentication token"
**Cause:** Token expired or invalid
**Solution:** 
- Refresh token using `/api/auth/refresh`
- Login again if refresh fails

### Issue: "Email address already exists"
**Cause:** Email already registered
**Solution:**
- Use different email
- Or login with existing account

### Issue: "CORS error"
**Cause:** Frontend and backend on different origins
**Solution:**
- Configure CORS on backend
- Use proxy in development

### Issue: "File upload failed"
**Cause:** File too large or invalid type
**Solution:**
- Check file size (<5MB)
- Check file type (JPEG, PNG, GIF, WebP)
- Check ImageKit configuration

---

## Support

For issues or questions:
- Check backend logs: `docker compose logs backend`
- Check browser console for errors
- Verify request format matches documentation
- Check error code and message for details

---

## Changelog

### Version 1.0.0 (2024-12-20)
- Initial release
- Email/Password authentication
- Firebase authentication (Google, Facebook)
- JWT token management
- User profile management
- Avatar upload
- Password reset via OTP

