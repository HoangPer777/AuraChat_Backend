# Firebase Authentication API

## Overview
API endpoints cho Firebase Authentication (Google và Facebook login).

## Endpoint

### Firebase Login
**POST** `/api/auth/firebase/login`

Xác thực người dùng bằng Firebase ID token từ client-side authentication.

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE5..."
}
```

**Response Success (200):**
```json
{
  "success": true,
  "message": "Firebase login successful",
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

**Response Error - Invalid Token (401):**
```json
{
  "success": false,
  "message": "Invalid username or password: Invalid Firebase ID token: Token has expired",
  "errorCode": "AUTH_003",
  "errorId": "ERR_20241201_123456_ABC",
  "details": null,
  "timestamp": "2024-12-01T12:34:56.789"
}
```

**Response Error - Validation (400):**
```json
{
  "success": false,
  "message": "Firebase ID token is required",
  "errorCode": "VAL_002",
  "errorId": "ERR_20241201_123456_DEF",
  "details": {
    "idToken": "Firebase ID token is required"
  },
  "timestamp": "2024-12-01T12:34:56.789"
}
```

## Authentication Flow

### 1. Client-Side (Frontend)

#### Google Login
```javascript
import { signInWithPopup, GoogleAuthProvider, getAuth } from 'firebase/auth';

const auth = getAuth();
const provider = new GoogleAuthProvider();

try {
  const result = await signInWithPopup(auth, provider);
  const idToken = await result.user.getIdToken();
  
  // Send idToken to backend
  const response = await fetch('/api/auth/firebase/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ idToken })
  });
  
  const data = await response.json();
  console.log('Login successful:', data);
} catch (error) {
  console.error('Login failed:', error);
}
```

#### Facebook Login
```javascript
import { signInWithPopup, FacebookAuthProvider, getAuth } from 'firebase/auth';

const auth = getAuth();
const provider = new FacebookAuthProvider();

try {
  const result = await signInWithPopup(auth, provider);
  const idToken = await result.user.getIdToken();
  
  // Send idToken to backend
  const response = await fetch('/api/auth/firebase/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ idToken })
  });
  
  const data = await response.json();
  console.log('Login successful:', data);
} catch (error) {
  console.error('Login failed:', error);
}
```

### 2. Server-Side (Backend)

1. Nhận Firebase ID token từ request
2. Verify token bằng Firebase Admin SDK
3. Extract user information (UID, email, name, picture)
4. Tìm user theo Firebase UID hoặc email
5. Tạo user mới nếu chưa tồn tại
6. Cập nhật thông tin user nếu cần
7. Generate JWT access token và refresh token
8. Return tokens và user info

## User Management

### New User Creation
Khi user đăng nhập lần đầu:
- Tạo User document mới trong MongoDB
- Set `provider` = "GOOGLE" hoặc "FACEBOOK"
- Set `providerId` = Firebase UID
- Set `email`, `displayName`, `avatarUrl` từ Firebase token
- Không set `passwordHash` (OAuth users không có password)

### Existing User Update
Khi user đã tồn tại:
- Cập nhật `displayName` nếu thay đổi
- Cập nhật `avatarUrl` nếu thay đổi
- Không thay đổi `email` (immutable)

### Account Linking
Nếu user đã có account với email tương tự:
- Link Firebase UID với existing account
- Update `provider` và `providerId`
- Preserve existing user data

## Error Handling

### Firebase Token Errors
- **Token expired**: `AUTH_003` - Token has expired
- **Invalid token**: `AUTH_003` - Invalid Firebase ID token
- **Token verification failed**: `AUTH_003` - Firebase verification error

### Validation Errors
- **Missing token**: `VAL_002` - Firebase ID token is required
- **Empty token**: `VAL_002` - Firebase ID token cannot be empty

### Business Logic Errors
- **Email required**: `VAL_002` - Email is required for user registration
- **User creation failed**: `SYS_001` - Database error during user creation

## Testing

### Manual Testing with cURL

1. Get Firebase ID token from frontend (browser console)
2. Test backend endpoint:

```bash
curl -X POST http://localhost:8080/api/auth/firebase/login \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "your-firebase-id-token-here"
  }'
```

### Frontend Testing

```html
<!DOCTYPE html>
<html>
<head>
    <script type="module">
        import { initializeApp } from 'https://www.gstatic.com/firebasejs/9.0.0/firebase-app.js';
        import { getAuth, signInWithPopup, GoogleAuthProvider } from 'https://www.gstatic.com/firebasejs/9.0.0/firebase-auth.js';

        // Your Firebase config
        const firebaseConfig = {
            apiKey: "your-api-key",
            authDomain: "your-project.firebaseapp.com",
            projectId: "your-project-id"
        };

        const app = initializeApp(firebaseConfig);
        const auth = getAuth();

        window.loginWithGoogle = async () => {
            const provider = new GoogleAuthProvider();
            try {
                const result = await signInWithPopup(auth, provider);
                const idToken = await result.user.getIdToken();
                
                console.log('Firebase ID Token:', idToken);
                
                const response = await fetch('http://localhost:8080/api/auth/firebase/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ idToken })
                });
                
                const data = await response.json();
                console.log('Backend Response:', data);
            } catch (error) {
                console.error('Error:', error);
            }
        };
    </script>
</head>
<body>
    <button onclick="loginWithGoogle()">Login with Google</button>
</body>
</html>
```

## Security Considerations

### Token Validation
- Firebase ID tokens are automatically validated by Firebase Admin SDK
- Tokens expire after 1 hour
- Backend verifies token signature, issuer, and audience
- Invalid or expired tokens are rejected

### User Data Protection
- Firebase UID is used as unique identifier
- Email addresses are validated by Firebase
- Profile pictures are served from Google/Facebook CDN
- No sensitive data is stored in Firebase tokens

### Rate Limiting
Consider implementing rate limiting for login endpoints:
- Max 10 login attempts per IP per minute
- Max 5 failed attempts per email per hour
- Temporary account lockout after repeated failures

## Provider Mapping

| Firebase Provider | Internal Provider | Description |
|------------------|------------------|-------------|
| `google.com` | `GOOGLE` | Google OAuth |
| `facebook.com` | `FACEBOOK` | Facebook OAuth |
| `password` | `FIREBASE_EMAIL` | Firebase Email/Password |
| Others | `FIREBASE` | Other Firebase providers |

## Database Schema

### User Document
```javascript
{
  "_id": ObjectId("..."),
  "email": "user@example.com",
  "displayName": "John Doe",
  "avatarUrl": "https://lh3.googleusercontent.com/...",
  "bio": null,
  "provider": "GOOGLE",           // GOOGLE | FACEBOOK | FIREBASE
  "providerId": "firebase-uid",   // Firebase UID
  "role": "USER",
  "status": "ACTIVE",
  "lastSeen": ISODate("..."),
  "createdAt": ISODate("..."),
  "updatedAt": ISODate("...")
}
```

### Indexes
- `email` (unique)
- `provider + providerId` (compound, unique)
- `providerId` (for Firebase UID lookup)

## Troubleshooting

### "Invalid Firebase ID token"
- Check token is not expired (1 hour limit)
- Verify Firebase project configuration
- Ensure service account key is correct
- Check network connectivity to Firebase

### "Email is required for user registration"
- Firebase token doesn't contain email
- Check Firebase Authentication settings
- Verify OAuth provider configuration
- Ensure email scope is requested

### "User creation failed"
- Database connection issues
- MongoDB write permissions
- Duplicate email constraint violation
- Check application logs for details