# Firebase Authentication Setup

## Overview
Hệ thống sử dụng Firebase Admin SDK để xác thực token từ Firebase Authentication (Google và Facebook login).

## Configuration

### 1. Firebase Console Setup

1. Truy cập [Firebase Console](https://console.firebase.google.com)
2. Chọn project hoặc tạo project mới
3. Vào **Project Settings** → **Service Accounts**
4. Click **Generate New Private Key**
5. Download file JSON và đặt tên là `serviceAccountKey.json`

### 2. File Placement

Có 3 cách đặt file `serviceAccountKey.json`:

#### Option 1: Trong resources folder (Recommended for development)
```
src/main/resources/serviceAccountKey.json
```
Set biến môi trường:
```env
FIREBASE_SERVICE_ACCOUNT_KEY_PATH=src/main/resources/serviceAccountKey.json
```

#### Option 2: Sử dụng classpath
```
src/main/resources/firebase/serviceAccountKey.json
```
Set biến môi trường:
```env
FIREBASE_SERVICE_ACCOUNT_KEY_PATH=classpath:firebase/serviceAccountKey.json
```

#### Option 3: Absolute path (Recommended for production)
```
/etc/aurachat/serviceAccountKey.json
```
Set biến môi trường:
```env
FIREBASE_SERVICE_ACCOUNT_KEY_PATH=/etc/aurachat/serviceAccountKey.json
```

### 3. Environment Variables

Thêm vào file `.env`:
```env
FIREBASE_SERVICE_ACCOUNT_KEY_PATH=src/main/resources/serviceAccountKey.json
```

### 4. Application Configuration

File `application.yml` đã được cấu hình:
```yaml
firebase:
  service-account-key-path: ${FIREBASE_SERVICE_ACCOUNT_KEY_PATH:src/main/resources/serviceAccountKey.json}
```

Default value là `src/main/resources/serviceAccountKey.json` nếu biến môi trường không được set.

## Firebase Authentication Flow

### Client-Side (Frontend)

1. User click "Login with Google" hoặc "Login with Facebook"
2. Firebase Authentication SDK xử lý OAuth flow
3. Nhận Firebase ID Token từ Firebase
4. Gửi ID Token đến backend API

### Server-Side (Backend)

1. Nhận Firebase ID Token từ request
2. Verify token bằng Firebase Admin SDK
3. Extract user information (email, name, photo, etc.)
4. Tạo hoặc update user trong database
5. Generate JWT access token và refresh token
6. Return tokens cho client

## Implementation

### Verify Firebase Token

```java
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

public String verifyFirebaseToken(String idToken) {
    try {
        FirebaseToken decodedToken = FirebaseAuth.getInstance()
            .verifyIdToken(idToken);
        String uid = decodedToken.getUid();
        String email = decodedToken.getEmail();
        String name = decodedToken.getName();
        String picture = decodedToken.getPicture();
        
        // Process user information
        return uid;
    } catch (FirebaseAuthException e) {
        throw new AuthenticationException(
            ErrorCode.AUTH_TOKEN_INVALID,
            "Invalid Firebase token",
            "firebase authentication"
        );
    }
}
```

### API Endpoint Example

**POST** `/api/auth/firebase/login`

**Request:**
```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE5..."
}
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "user": {
      "id": "507f1f77bcf86cd799439011",
      "email": "user@example.com",
      "displayName": "John Doe",
      "avatarUrl": "https://lh3.googleusercontent.com/...",
      "bio": null
    }
  },
  "timestamp": "2024-12-01T12:34:56.789"
}
```

## Security Considerations

### 1. Service Account Key Protection

**DO NOT:**
- ❌ Commit `serviceAccountKey.json` to Git
- ❌ Share service account key publicly
- ❌ Store in client-side code

**DO:**
- ✅ Add `serviceAccountKey.json` to `.gitignore`
- ✅ Use environment variables for path
- ✅ Restrict file permissions (chmod 600)
- ✅ Use different keys for dev/staging/production

### 2. Token Validation

- Always verify Firebase ID token on server-side
- Check token expiration
- Validate token issuer and audience
- Handle token revocation

### 3. Production Deployment

For production, consider:
- Store service account key in secure vault (AWS Secrets Manager, Azure Key Vault, etc.)
- Use IAM roles instead of service account keys (when possible)
- Rotate service account keys regularly
- Monitor Firebase Admin SDK usage

## Troubleshooting

### Error: "Failed to initialize Firebase"

**Cause:** Service account key file not found

**Solution:**
1. Check file path in environment variable
2. Verify file exists at specified location
3. Check file permissions
4. Ensure file is valid JSON

### Error: "Invalid Firebase token"

**Cause:** Token verification failed

**Solution:**
1. Check token is not expired
2. Verify token is from correct Firebase project
3. Ensure Firebase Admin SDK is initialized
4. Check network connectivity to Firebase

### Error: "Permission denied"

**Cause:** Service account lacks required permissions

**Solution:**
1. Go to Firebase Console → IAM & Admin
2. Ensure service account has "Firebase Admin SDK Administrator Service Agent" role
3. Regenerate service account key if needed

## Testing

### Manual Testing with cURL

1. Get Firebase ID token from frontend
2. Test backend endpoint:

```bash
curl -X POST http://localhost:8080/api/auth/firebase/login \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "your-firebase-id-token-here"
  }'
```

### Integration Testing

```java
@Test
void testFirebaseLogin() {
    // Mock Firebase token verification
    when(firebaseAuth.verifyIdToken(anyString()))
        .thenReturn(mockFirebaseToken);
    
    // Test login endpoint
    FirebaseLoginRequest request = new FirebaseLoginRequest("mock-token");
    AuthResponse response = authService.loginWithFirebase(request);
    
    assertNotNull(response.getAccessToken());
    assertNotNull(response.getUser());
}
```

## References

- [Firebase Admin SDK Documentation](https://firebase.google.com/docs/admin/setup)
- [Firebase Authentication](https://firebase.google.com/docs/auth)
- [Verify ID Tokens](https://firebase.google.com/docs/auth/admin/verify-id-tokens)
