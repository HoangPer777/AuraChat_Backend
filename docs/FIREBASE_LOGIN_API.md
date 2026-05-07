# Firebase Authentication API Documentation

## Overview

AuraChat backend hỗ trợ đăng nhập bằng **Google** và **Facebook** thông qua Firebase Authentication. Frontend cần lấy Firebase ID Token từ Firebase SDK, sau đó gửi token này đến backend để nhận JWT tokens.

---

## Architecture Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (React/Vue)                     │
│  1. User clicks "Login with Google/Facebook"                    │
│  2. Firebase SDK opens popup/redirect                           │
│  3. User authenticates with Google/Facebook                     │
│  4. Firebase returns ID Token                                   │
│  5. Frontend sends ID Token to backend                          │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Backend (Spring Boot)                         │
│  1. Receive Firebase ID Token                                   │
│  2. Verify token with Firebase Admin SDK                        │
│  3. Extract user info (email, name, picture, provider)          │
│  4. Find or create user in MongoDB                              │
│  5. Generate JWT tokens (access + refresh)                      │
│  6. Return JWT tokens to frontend                               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Frontend (React/Vue)                          │
│  1. Store JWT tokens (localStorage/sessionStorage)              │
│  2. Use access token for authenticated requests                 │
│  3. Use refresh token to get new access token when expired      │
└─────────────────────────────────────────────────────────────────┘
```

---

## Endpoint: Firebase Login

### Request

**URL:** `POST /api/auth/firebase/login`

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEyMzQ1Njc4OTAiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL3NlY3VyZXRva2VuLmdvb2dsZS5jb20vYXVyYWNoYXQiLCJhdWQiOiJhdXJhY2hhdCIsImF1dGhfdGltZSI6MTcwMzA2MDAwMCwiZXhwIjoxNzAzMDYzNjAwLCJmaXJlYmFzZSI6eyJpZGVudGl0aWVzIjp7Imdvb2dsZS5jb20iOlsiMTIzNDU2Nzg5MCJdfSwic2lnbl9pbl9wcm92aWRlciI6Imdvb2dsZS5jb20ifSwiaWF0IjoxNzAzMDYwMDAwLCJlbWFpbCI6InVzZXJAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsicGhvbmVfbnVtYmVyIjpbXSwiZW1haWwiOlsidXNlckBnbWFpbC5jb20iXX0sInNpZ25faW5fcHJvdmlkZXIiOiJnb29nbGUuY29tIn0sInVpZCI6ImFiY2RlZjEyMzQ1Njc4OTAifQ.signature..."
}
```

**Parameters:**
- `idToken` (string, required): Firebase ID Token từ Firebase SDK

---

### Response

**Status Code:** `200 OK`

**Body:**
```json
{
  "success": true,
  "message": "Firebase login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI2NWE4YzQyZTEyMzQ1Njc4OTAiLCJpYXQiOjE3MDMwNjAwMDAsImV4cCI6MTcwMzA2MzYwMH0.signature...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "user": {
      "id": "65a8c42e1234567890",
      "email": "user@gmail.com",
      "displayName": "John Doe",
      "avatarUrl": "https://lh3.googleusercontent.com/a/default-user=s96-c",
      "provider": "GOOGLE",
      "createdAt": "2024-12-20T10:00:00Z",
      "updatedAt": "2024-12-20T10:00:00Z"
    }
  },
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**Response Fields:**
- `success` (boolean): Luôn là `true` nếu login thành công
- `message` (string): Mô tả kết quả
- `data` (object): Chứa tokens và user info
  - `accessToken` (string): JWT token để sử dụng cho authenticated requests (hết hạn sau 1 giờ)
  - `refreshToken` (string): Token để refresh access token (hết hạn sau 7 ngày)
  - `user` (object): Thông tin user
    - `id` (string): User ID trong MongoDB
    - `email` (string): Email của user
    - `displayName` (string): Tên hiển thị
    - `avatarUrl` (string): URL ảnh đại diện từ Google/Facebook
    - `provider` (string): Nhà cung cấp xác thực (GOOGLE, FACEBOOK)
    - `createdAt` (string): Thời gian tạo tài khoản
    - `updatedAt` (string): Thời gian cập nhật gần nhất
- `timestamp` (string): Thời gian response (ISO 8601)

---

### Error Responses

**Status Code:** `401 Unauthorized`

```json
{
  "success": false,
  "message": "Invalid Firebase ID token: Token verification failed",
  "errorCode": "AUTH_003",
  "errorId": "ERR_20241220_100000_ABC123",
  "details": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

**Possible Error Codes:**
- `AUTH_003` - Invalid Firebase ID token
- `AUTH_001` - Authentication failed
- `VAL_002` - Required field missing (email)

---

## Frontend Implementation Guide

### 1. Setup Firebase SDK

**Install Firebase:**
```bash
npm install firebase
```

**Initialize Firebase (React example):**
```javascript
import { initializeApp } from 'firebase/app';
import { getAuth, signInWithPopup, GoogleAuthProvider, FacebookAuthProvider } from 'firebase/auth';

const firebaseConfig = {
  apiKey: "YOUR_API_KEY",
  authDomain: "your-project.firebaseapp.com",
  projectId: "your-project-id",
  storageBucket: "your-project.appspot.com",
  messagingSenderId: "123456789",
  appId: "1:123456789:web:abcdef123456"
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);

export { auth, GoogleAuthProvider, FacebookAuthProvider };
```

### 2. Login with Google

```javascript
import { signInWithPopup, GoogleAuthProvider } from 'firebase/auth';
import { auth } from './firebase-config';

async function loginWithGoogle() {
  try {
    const provider = new GoogleAuthProvider();
    const result = await signInWithPopup(auth, provider);
    
    // Get Firebase ID Token
    const idToken = await result.user.getIdToken();
    
    // Send to backend
    const response = await fetch('http://localhost:8080/api/auth/firebase/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ idToken })
    });
    
    const data = await response.json();
    
    if (data.success) {
      // Store tokens
      localStorage.setItem('accessToken', data.data.accessToken);
      localStorage.setItem('refreshToken', data.data.refreshToken);
      localStorage.setItem('user', JSON.stringify(data.data.user));
      
      // Redirect to home
      window.location.href = '/home';
    } else {
      console.error('Login failed:', data.message);
    }
  } catch (error) {
    console.error('Error:', error);
  }
}
```

### 3. Login with Facebook

```javascript
import { signInWithPopup, FacebookAuthProvider } from 'firebase/auth';
import { auth } from './firebase-config';

async function loginWithFacebook() {
  try {
    const provider = new FacebookAuthProvider();
    const result = await signInWithPopup(auth, provider);
    
    // Get Firebase ID Token
    const idToken = await result.user.getIdToken();
    
    // Send to backend (same as Google)
    const response = await fetch('http://localhost:8080/api/auth/firebase/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ idToken })
    });
    
    const data = await response.json();
    
    if (data.success) {
      localStorage.setItem('accessToken', data.data.accessToken);
      localStorage.setItem('refreshToken', data.data.refreshToken);
      localStorage.setItem('user', JSON.stringify(data.data.user));
      window.location.href = '/home';
    }
  } catch (error) {
    console.error('Error:', error);
  }
}
```

### 4. Using Access Token for Authenticated Requests

```javascript
async function fetchUserProfile() {
  const accessToken = localStorage.getItem('accessToken');
  
  const response = await fetch('http://localhost:8080/api/auth/me', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    }
  });
  
  return await response.json();
}
```

### 5. Refresh Access Token

```javascript
async function refreshAccessToken() {
  const refreshToken = localStorage.getItem('refreshToken');
  
  const response = await fetch('http://localhost:8080/api/auth/refresh', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ refreshToken })
  });
  
  const data = await response.json();
  
  if (data.success) {
    localStorage.setItem('accessToken', data.data.accessToken);
    localStorage.setItem('refreshToken', data.data.refreshToken);
    return data.data.accessToken;
  } else {
    // Refresh token expired, need to login again
    localStorage.clear();
    window.location.href = '/login';
  }
}
```

---

## Token Management

### Access Token
- **Lifetime:** 1 hour
- **Usage:** Include in `Authorization: Bearer <token>` header for authenticated requests
- **Refresh:** Use refresh token to get new access token

### Refresh Token
- **Lifetime:** 7 days
- **Usage:** Send to `/api/auth/refresh` endpoint to get new access token
- **Storage:** Store securely (httpOnly cookie recommended for production)

---

## User Information

### Provider Values
- `GOOGLE` - Logged in with Google
- `FACEBOOK` - Logged in with Facebook
- `FIREBASE_EMAIL` - Logged in with email/password

### User Fields
- `id` - MongoDB user ID
- `email` - User email
- `displayName` - User's display name
- `avatarUrl` - Profile picture URL
- `provider` - Authentication provider
- `createdAt` - Account creation timestamp
- `updatedAt` - Last update timestamp

---

## Common Issues & Solutions

### Issue 1: "Invalid Firebase ID token"
**Cause:** Token is expired or invalid
**Solution:** 
- Ensure token is freshly obtained from Firebase SDK
- Check Firebase project configuration
- Verify backend Firebase config is correct

### Issue 2: "CORS error"
**Cause:** Frontend and backend on different origins
**Solution:**
- Add CORS configuration to backend
- Or use proxy in development

### Issue 3: "Email is required"
**Cause:** Firebase token doesn't contain email
**Solution:**
- Ensure user email is verified in Firebase
- Check Firebase authentication provider settings

### Issue 4: "Token verification failed"
**Cause:** Backend Firebase config is incorrect
**Solution:**
- Verify `serviceAccountKey.json` path in backend
- Check `FIREBASE_SERVICE_ACCOUNT_KEY_PATH` environment variable
- Ensure Firebase Admin SDK is properly initialized

---

## Testing

### Using Postman/Insomnia

1. **Get Firebase ID Token:**
   - Use the test HTML page: `http://localhost:8080/test-firebase-login.html`
   - Or use Firebase Console to generate test token

2. **Test Login Endpoint:**
   ```
   POST http://localhost:8080/api/auth/firebase/login
   Content-Type: application/json
   
   {
     "idToken": "your-firebase-id-token-here"
   }
   ```

3. **Test Authenticated Endpoint:**
   ```
   GET http://localhost:8080/api/auth/me
   Authorization: Bearer your-access-token-here
   ```

---

## Related Endpoints

### Get User Profile
```
GET /api/auth/me
Authorization: Bearer <accessToken>
```

### Update User Profile
```
PATCH /api/auth/me
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "displayName": "New Name",
  "avatarUrl": "https://..."
}
```

### Upload Avatar
```
POST /api/auth/me/avatar
Authorization: Bearer <accessToken>
Content-Type: multipart/form-data

file: <image-file>
```

### Refresh Token
```
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "your-refresh-token"
}
```

### Logout
```
POST /api/auth/logout
Authorization: Bearer <accessToken>
```

---

## Security Considerations

1. **Store tokens securely:**
   - Use httpOnly cookies for production
   - Avoid storing in localStorage if possible
   - Never expose tokens in URLs

2. **HTTPS only:**
   - Always use HTTPS in production
   - Firebase requires HTTPS for authentication

3. **Token expiration:**
   - Access tokens expire after 1 hour
   - Implement automatic refresh before expiration
   - Handle 401 responses by refreshing token

4. **CORS:**
   - Configure CORS properly on backend
   - Only allow trusted origins

5. **Firebase Security Rules:**
   - Configure Firebase security rules
   - Restrict access to sensitive data

---

## Support

For issues or questions:
- Check backend logs: `docker compose logs backend`
- Check browser console for errors
- Verify Firebase configuration
- Test with provided HTML test page

