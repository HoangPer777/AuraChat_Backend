# Firebase Login - React Implementation Example

## Complete React Component Example

### 1. Firebase Configuration

**File: `src/config/firebase.js`**

```javascript
import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';

const firebaseConfig = {
  apiKey: process.env.REACT_APP_FIREBASE_API_KEY,
  authDomain: process.env.REACT_APP_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.REACT_APP_FIREBASE_PROJECT_ID,
  storageBucket: process.env.REACT_APP_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: process.env.REACT_APP_FIREBASE_MESSAGING_SENDER_ID,
  appId: process.env.REACT_APP_FIREBASE_APP_ID,
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
```

**File: `.env`**

```
REACT_APP_FIREBASE_API_KEY=your_api_key
REACT_APP_FIREBASE_AUTH_DOMAIN=your-project.firebaseapp.com
REACT_APP_FIREBASE_PROJECT_ID=your-project-id
REACT_APP_FIREBASE_STORAGE_BUCKET=your-project.appspot.com
REACT_APP_FIREBASE_MESSAGING_SENDER_ID=123456789
REACT_APP_FIREBASE_APP_ID=1:123456789:web:abcdef123456

REACT_APP_API_URL=http://localhost:8080
```

---

### 2. Auth Service

**File: `src/services/authService.js`**

```javascript
const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

export const authService = {
  /**
   * Login with Firebase ID Token
   */
  async firebaseLogin(idToken) {
    try {
      const response = await fetch(`${API_URL}/api/auth/firebase/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ idToken }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || 'Firebase login failed');
      }

      return data.data;
    } catch (error) {
      console.error('Firebase login error:', error);
      throw error;
    }
  },

  /**
   * Store tokens and user info
   */
  storeAuthData(authData) {
    localStorage.setItem('accessToken', authData.accessToken);
    localStorage.setItem('refreshToken', authData.refreshToken);
    localStorage.setItem('user', JSON.stringify(authData.user));
  },

  /**
   * Get stored access token
   */
  getAccessToken() {
    return localStorage.getItem('accessToken');
  },

  /**
   * Get stored refresh token
   */
  getRefreshToken() {
    return localStorage.getItem('refreshToken');
  },

  /**
   * Get stored user info
   */
  getUser() {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  },

  /**
   * Refresh access token
   */
  async refreshAccessToken() {
    try {
      const refreshToken = this.getRefreshToken();
      if (!refreshToken) {
        throw new Error('No refresh token available');
      }

      const response = await fetch(`${API_URL}/api/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ refreshToken }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || 'Token refresh failed');
      }

      this.storeAuthData(data.data);
      return data.data.accessToken;
    } catch (error) {
      console.error('Token refresh error:', error);
      this.logout();
      throw error;
    }
  },

  /**
   * Logout
   */
  logout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
  },

  /**
   * Check if user is authenticated
   */
  isAuthenticated() {
    return !!this.getAccessToken();
  },
};
```

---

### 3. Login Component

**File: `src/components/FirebaseLogin.jsx`**

```javascript
import React, { useState } from 'react';
import { signInWithPopup, GoogleAuthProvider, FacebookAuthProvider } from 'firebase/auth';
import { auth } from '../config/firebase';
import { authService } from '../services/authService';
import './FirebaseLogin.css';

export const FirebaseLogin = ({ onLoginSuccess }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleGoogleLogin = async () => {
    try {
      setLoading(true);
      setError(null);

      const provider = new GoogleAuthProvider();
      const result = await signInWithPopup(auth, provider);

      // Get Firebase ID Token
      const idToken = await result.user.getIdToken();

      // Send to backend
      const authData = await authService.firebaseLogin(idToken);

      // Store tokens and user info
      authService.storeAuthData(authData);

      // Callback
      if (onLoginSuccess) {
        onLoginSuccess(authData.user);
      }
    } catch (err) {
      console.error('Google login error:', err);
      setError(err.message || 'Google login failed');
    } finally {
      setLoading(false);
    }
  };

  const handleFacebookLogin = async () => {
    try {
      setLoading(true);
      setError(null);

      const provider = new FacebookAuthProvider();
      const result = await signInWithPopup(auth, provider);

      // Get Firebase ID Token
      const idToken = await result.user.getIdToken();

      // Send to backend
      const authData = await authService.firebaseLogin(idToken);

      // Store tokens and user info
      authService.storeAuthData(authData);

      // Callback
      if (onLoginSuccess) {
        onLoginSuccess(authData.user);
      }
    } catch (err) {
      console.error('Facebook login error:', err);
      setError(err.message || 'Facebook login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="firebase-login">
      <h2>Login with Social Account</h2>

      {error && <div className="error-message">{error}</div>}

      <div className="button-group">
        <button
          className="btn btn-google"
          onClick={handleGoogleLogin}
          disabled={loading}
        >
          {loading ? 'Loading...' : '🔵 Login with Google'}
        </button>

        <button
          className="btn btn-facebook"
          onClick={handleFacebookLogin}
          disabled={loading}
        >
          {loading ? 'Loading...' : '🔵 Login with Facebook'}
        </button>
      </div>
    </div>
  );
};
```

**File: `src/components/FirebaseLogin.css`**

```css
.firebase-login {
  max-width: 400px;
  margin: 0 auto;
  padding: 20px;
}

.firebase-login h2 {
  text-align: center;
  margin-bottom: 20px;
  color: #333;
}

.error-message {
  background: #f8d7da;
  color: #721c24;
  padding: 12px;
  border-radius: 5px;
  margin-bottom: 15px;
  border-left: 4px solid #dc3545;
}

.button-group {
  display: flex;
  gap: 10px;
  flex-direction: column;
}

.btn {
  padding: 12px 20px;
  border: none;
  border-radius: 5px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-google {
  background: #4285F4;
  color: white;
}

.btn-google:hover:not(:disabled) {
  background: #357ae8;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(66, 133, 244, 0.4);
}

.btn-facebook {
  background: #1877F2;
  color: white;
}

.btn-facebook:hover:not(:disabled) {
  background: #0a66c2;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(24, 119, 242, 0.4);
}
```

---

### 4. Protected Route Component

**File: `src/components/ProtectedRoute.jsx`**

```javascript
import React from 'react';
import { Navigate } from 'react-router-dom';
import { authService } from '../services/authService';

export const ProtectedRoute = ({ children }) => {
  if (!authService.isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  return children;
};
```

---

### 5. API Client with Token Management

**File: `src/services/apiClient.js`**

```javascript
import { authService } from './authService';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

class ApiClient {
  async request(endpoint, options = {}) {
    const url = `${API_URL}${endpoint}`;
    const headers = {
      'Content-Type': 'application/json',
      ...options.headers,
    };

    // Add access token if available
    const accessToken = authService.getAccessToken();
    if (accessToken) {
      headers['Authorization'] = `Bearer ${accessToken}`;
    }

    let response = await fetch(url, {
      ...options,
      headers,
    });

    // If 401, try to refresh token
    if (response.status === 401 && accessToken) {
      try {
        const newAccessToken = await authService.refreshAccessToken();
        headers['Authorization'] = `Bearer ${newAccessToken}`;

        response = await fetch(url, {
          ...options,
          headers,
        });
      } catch (error) {
        // Refresh failed, redirect to login
        window.location.href = '/login';
        throw error;
      }
    }

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || 'API request failed');
    }

    return data;
  }

  get(endpoint, options = {}) {
    return this.request(endpoint, { ...options, method: 'GET' });
  }

  post(endpoint, body, options = {}) {
    return this.request(endpoint, {
      ...options,
      method: 'POST',
      body: JSON.stringify(body),
    });
  }

  patch(endpoint, body, options = {}) {
    return this.request(endpoint, {
      ...options,
      method: 'PATCH',
      body: JSON.stringify(body),
    });
  }

  delete(endpoint, options = {}) {
    return this.request(endpoint, { ...options, method: 'DELETE' });
  }
}

export const apiClient = new ApiClient();
```

---

### 6. User Profile Component

**File: `src/components/UserProfile.jsx`**

```javascript
import React, { useState, useEffect } from 'react';
import { apiClient } from '../services/apiClient';
import { authService } from '../services/authService';

export const UserProfile = () => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchUserProfile();
  }, []);

  const fetchUserProfile = async () => {
    try {
      setLoading(true);
      const response = await apiClient.get('/api/auth/me');
      setUser(response.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    authService.logout();
    window.location.href = '/login';
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;
  if (!user) return <div>No user data</div>;

  return (
    <div className="user-profile">
      <h2>User Profile</h2>

      {user.avatarUrl && (
        <img src={user.avatarUrl} alt="Avatar" className="avatar" />
      )}

      <div className="profile-info">
        <p>
          <strong>Name:</strong> {user.displayName}
        </p>
        <p>
          <strong>Email:</strong> {user.email}
        </p>
        <p>
          <strong>Provider:</strong> {user.provider}
        </p>
        <p>
          <strong>Joined:</strong> {new Date(user.createdAt).toLocaleDateString()}
        </p>
      </div>

      <button onClick={handleLogout} className="btn btn-logout">
        Logout
      </button>
    </div>
  );
};
```

---

### 7. Login Page

**File: `src/pages/LoginPage.jsx`**

```javascript
import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { FirebaseLogin } from '../components/FirebaseLogin';
import { authService } from '../services/authService';

export const LoginPage = () => {
  const navigate = useNavigate();

  useEffect(() => {
    // If already logged in, redirect to home
    if (authService.isAuthenticated()) {
      navigate('/home');
    }
  }, [navigate]);

  const handleLoginSuccess = (user) => {
    console.log('Login successful:', user);
    navigate('/home');
  };

  return (
    <div className="login-page">
      <div className="login-container">
        <h1>Welcome to AuraChat</h1>
        <FirebaseLogin onLoginSuccess={handleLoginSuccess} />
      </div>
    </div>
  );
};
```

---

### 8. App Router Setup

**File: `src/App.jsx`**

```javascript
import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { LoginPage } from './pages/LoginPage';
import { HomePage } from './pages/HomePage';
import { ProtectedRoute } from './components/ProtectedRoute';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/home"
          element={
            <ProtectedRoute>
              <HomePage />
            </ProtectedRoute>
          }
        />
        <Route path="/" element={<Navigate to="/home" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
```

---

## Installation & Setup

### 1. Install Dependencies

```bash
npm install firebase react-router-dom
```

### 2. Setup Environment Variables

Create `.env` file with Firebase credentials from Firebase Console.

### 3. Initialize Firebase in Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a new project or select existing
3. Enable Google and Facebook authentication
4. Get your Firebase config
5. Add authorized domains (localhost:3000 for development)

### 4. Run the App

```bash
npm start
```

---

## Testing Flow

1. **Start Backend:**
   ```bash
   docker compose up --build
   ```

2. **Start Frontend:**
   ```bash
   npm start
   ```

3. **Test Login:**
   - Go to http://localhost:3000/login
   - Click "Login with Google" or "Login with Facebook"
   - Authenticate with your account
   - Should redirect to home page
   - Check localStorage for tokens

4. **Test Protected Route:**
   - Try accessing /home without logging in
   - Should redirect to /login

5. **Test Token Refresh:**
   - Wait for access token to expire (1 hour)
   - Make an API request
   - Should automatically refresh token

---

## Troubleshooting

### Issue: "Firebase config is not valid"
- Check `.env` file has correct Firebase credentials
- Verify Firebase project is created

### Issue: "CORS error"
- Backend CORS is configured for localhost:3000
- Check backend logs for CORS issues

### Issue: "Login popup blocked"
- Some browsers block popups
- Check browser popup settings
- Use redirect flow instead of popup

### Issue: "Token not stored"
- Check browser localStorage is enabled
- Check browser console for errors
- Verify authService.storeAuthData is called

---

## Security Best Practices

1. **Never commit `.env` file** - Add to `.gitignore`
2. **Use environment variables** for sensitive data
3. **HTTPS only in production** - Firebase requires HTTPS
4. **Secure token storage** - Consider using httpOnly cookies
5. **Validate tokens on backend** - Always verify tokens
6. **Implement token refresh** - Refresh before expiration
7. **Handle 401 responses** - Redirect to login on auth failure

