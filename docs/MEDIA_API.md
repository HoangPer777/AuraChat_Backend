# Media API Documentation

## Overview
This document describes media upload APIs.

---

## Base URL

```
http://localhost:8080
```

---

## Authentication
All endpoints require a valid JWT access token.

**Header:**
```
Authorization: Bearer <access_token>
```

---

## Rate Limiting
- 10 uploads per hour per user

---

## Response Format

Success:
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {},
  "timestamp": "2024-12-20T10:00:00Z"
}
```

Error:
```json
{
  "success": false,
  "message": "Error message",
  "errorCode": "ERROR_CODE",
  "errorId": "ERR_20241220_100000_ABC123",
  "details": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

---

## REST Endpoints

### 1) Upload Image
**POST** `/api/media/upload/image`

**Content-Type:** `multipart/form-data`

**Form Fields:**
- `file` (file, required)

**Validation:**
- Max size: 10MB
- Image types: jpg, png, gif, webp
- Validates content type and extension
- Rejects executables: exe, sh, bat

**Response 200:**
```json
{
  "success": true,
  "message": "Image uploaded successfully",
  "data": {
    "url": "https://ik.imagekit.io/your_id/media_...",
    "fileName": "media_user_abcdef12.png",
    "originalFileName": "photo.png",
    "contentType": "image/png",
    "size": 12345,
    "provider": "ImageKit"
  },
  "timestamp": "2024-12-20T10:00:00Z"
}
```

---

### 2) Upload File
**POST** `/api/media/upload/file`

**Content-Type:** `multipart/form-data`

**Form Fields:**
- `file` (file, required)

**Validation:**
- Max size: 10MB
- File types: pdf, docx, xlsx, txt
- Validates content type and extension
- Rejects executables: exe, sh, bat

**Response 200:**
```json
{
  "success": true,
  "message": "File uploaded successfully",
  "data": {
    "url": "https://ik.imagekit.io/your_id/media_...",
    "fileName": "media_user_abcdef12.pdf",
    "originalFileName": "doc.pdf",
    "contentType": "application/pdf",
    "size": 12345,
    "provider": "ImageKit"
  },
  "timestamp": "2024-12-20T10:00:00Z"
}
```
