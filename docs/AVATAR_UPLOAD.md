# Avatar Upload Feature

## Overview
Chức năng upload avatar cho phép người dùng tải lên ảnh đại diện của họ. Hệ thống sẽ lưu trữ ảnh trên ImageKit trong thư mục `Home/aurachat/avatar` và cập nhật URL vào database.

## API Endpoint

### Upload Avatar
**POST** `/api/auth/me/avatar`

**Headers:**
```
Authorization: Bearer <access_token>
Content-Type: multipart/form-data
```

**Request:**
- Form data với key `file` chứa file ảnh

**Response Success (200):**
```json
{
  "success": true,
  "message": "Avatar uploaded successfully",
  "data": "https://ik.imagekit.io/6dozttd2u/Home/aurachat/avatar/avatar_userId_abc12345.jpg",
  "timestamp": "2024-12-01T12:34:56.789"
}
```

**Response Error - File quá lớn (400):**
```json
{
  "success": false,
  "message": "File size exceeds maximum allowed size of 5 MB",
  "errorCode": "MEDIA_003",
  "errorId": "ERR_20241201_123456_ABC",
  "details": {
    "field": "file",
    "invalidValue": 6291456
  },
  "timestamp": "2024-12-01T12:34:56.789"
}
```

**Response Error - File type không hợp lệ (400):**
```json
{
  "success": false,
  "message": "Invalid file type. Allowed types: JPEG, PNG, GIF, WebP",
  "errorCode": "MEDIA_002",
  "errorId": "ERR_20241201_123456_DEF",
  "details": {
    "field": "file",
    "invalidValue": "application/pdf"
  },
  "timestamp": "2024-12-01T12:34:56.789"
}
```

**Response Error - Upload thất bại (422):**
```json
{
  "success": false,
  "message": "Failed to upload media file",
  "errorCode": "MEDIA_001",
  "errorId": "ERR_20241201_123456_GHI",
  "details": null,
  "timestamp": "2024-12-01T12:34:56.789"
}
```

## Validation Rules

### File Size
- **Maximum:** 5 MB
- **Error Code:** `MEDIA_003` (MEDIA_SIZE_EXCEEDED)

### File Types
- **Allowed:** JPEG, JPG, PNG, GIF, WebP
- **Error Code:** `MEDIA_002` (MEDIA_INVALID_TYPE)

### Required Field
- File không được null hoặc empty
- **Error Code:** `VAL_002` (VALIDATION_REQUIRED_FIELD)

## ImageKit Configuration

### Folder Structure
Tất cả avatar được lưu trong: `Home/aurachat/avatar`

### File Naming Convention
Format: `avatar_{userId}_{randomId}.{extension}`

Ví dụ: `avatar_507f1f77bcf86cd799439011_abc12345.jpg`

### Environment Variables
```env
IMAGEKIT_URL_ENDPOINT=https://ik.imagekit.io/your-id
IMAGEKIT_PUBLIC_KEY=public_xxxxx
IMAGEKIT_PRIVATE_KEY=private_xxxxx
```

## Usage Examples

### cURL
```bash
curl -X POST http://localhost:8080/api/auth/me/avatar \
  -H "Authorization: Bearer your_access_token" \
  -F "file=@/path/to/avatar.jpg"
```

### JavaScript (Fetch API)
```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);

fetch('http://localhost:8080/api/auth/me/avatar', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`
  },
  body: formData
})
.then(response => response.json())
.then(data => {
  console.log('Avatar URL:', data.data);
})
.catch(error => console.error('Error:', error));
```

### React Example
```jsx
const handleAvatarUpload = async (event) => {
  const file = event.target.files[0];
  
  if (!file) return;
  
  const formData = new FormData();
  formData.append('file', file);
  
  try {
    const response = await fetch('/api/auth/me/avatar', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${accessToken}`
      },
      body: formData
    });
    
    const result = await response.json();
    
    if (result.success) {
      console.log('Avatar uploaded:', result.data);
      // Update UI with new avatar URL
    } else {
      console.error('Upload failed:', result.message);
    }
  } catch (error) {
    console.error('Error uploading avatar:', error);
  }
};

// JSX
<input 
  type="file" 
  accept="image/jpeg,image/png,image/gif,image/webp"
  onChange={handleAvatarUpload}
/>
```

## Flow Diagram

```
User → Upload File → Validation → ImageKit Upload → Get URL → Update DB → Return URL
                         ↓
                    [Validation Errors]
                         ↓
                    Return Error Response
```

## Implementation Details

### Components

1. **ImageKitConfig** (`config/ImageKitConfig.java`)
   - Khởi tạo ImageKit SDK với credentials
   - Bean configuration cho Spring

2. **AvatarUploadService** (`module/auth/service/AvatarUploadService.java`)
   - Validate file (size, type)
   - Upload to ImageKit
   - Update user avatar URL in database
   - Error handling với custom exceptions

3. **AuthController** (`module/auth/controller/AuthController.java`)
   - Endpoint `/api/auth/me/avatar`
   - Multipart file handling
   - Authentication required

### Error Handling

Tất cả errors được xử lý bằng custom exceptions:
- `ValidationException` - File validation errors
- `SystemException` - ImageKit upload errors
- `BusinessLogicException` - User not found errors

### Security

- Endpoint yêu cầu authentication (JWT token)
- Chỉ user đã login mới có thể upload avatar
- File size và type được validate để tránh abuse

## Testing

### Manual Testing
1. Login để lấy access token
2. Sử dụng Postman hoặc cURL để upload file
3. Verify URL được trả về
4. Check database để confirm avatar URL đã được update

### Test Cases
- ✅ Upload valid image (JPEG, PNG, GIF, WebP)
- ✅ Upload file quá lớn (> 5MB)
- ✅ Upload file type không hợp lệ (PDF, TXT, etc.)
- ✅ Upload without authentication
- ✅ Upload empty file
- ✅ Upload với user không tồn tại

## Notes

- Avatar URL được lưu trực tiếp vào User document trong MongoDB
- Không có chức năng xóa avatar cũ trên ImageKit (có thể implement sau)
- File được rename với UUID để tránh conflict
- ImageKit tự động optimize và serve images với CDN
