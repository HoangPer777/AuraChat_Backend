# Media API Documentation

## Overview
This document describes media APIs for user library management (Create + Read + Delete).

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
- 10 uploads per hour per user (only applies to upload endpoints)

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

---

### 3) List User Media
**GET** `/api/media?page=0&size=20`

Returns media uploaded by the authenticated user only.

**Response 200:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "665f1d4c98d6cf3a1f6a7ab2",
        "fileId": "ik_file_abc123",
        "url": "https://ik.imagekit.io/your_id/media_user_abc123.png",
        "fileName": "media_user_abc123.png",
        "originalFileName": "photo.png",
        "contentType": "image/png",
        "size": 12345,
        "provider": "ImageKit",
        "mediaType": "IMAGE",
        "createdAt": "2026-06-23T10:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

---

### 4) Get Media Detail
**GET** `/api/media/{mediaId}`

Returns one media item if it belongs to the authenticated user.

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": "665f1d4c98d6cf3a1f6a7ab2",
    "fileId": "ik_file_abc123",
    "url": "https://ik.imagekit.io/your_id/media_user_abc123.png",
    "fileName": "media_user_abc123.png",
    "originalFileName": "photo.png",
    "contentType": "image/png",
    "size": 12345,
    "provider": "ImageKit",
    "mediaType": "IMAGE",
    "createdAt": "2026-06-23T10:00:00Z"
  }
}
```

---

### 5) Delete Media
**DELETE** `/api/media/{mediaId}`

Deletes the file from ImageKit and marks metadata as deleted.

**Response 200:**
```json
{
  "success": true,
  "message": "Media deleted successfully",
  "data": null
}
```
