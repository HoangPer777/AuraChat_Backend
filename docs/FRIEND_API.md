# Friend API Documentation

## Overview
This document describes friend-related REST APIs and WebSocket notifications.

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

### 1) Search Users
**GET** `/api/friends/search?q={query}`

**Query Params:**
- `q` (string, required): text to search in `displayName` or `email`

**Notes:**
- Excludes the current user
- Excludes existing friends
- Returns up to 50 results

**Response 200:**
```json
{
  "success": true,
  "message": "Search completed",
  "data": [
    {
      "id": "65a8c42e1234567890",
      "displayName": "John Doe",
      "email": "john@example.com",
      "avatarUrl": "https://example.com/avatar.jpg"
    }
  ],
  "timestamp": "2024-12-20T10:00:00Z"
}
```

---

### 2) Send Friend Request
**POST** `/api/friends/request`

**Rate limit:** 5 requests per minute per user

**Body:**
```json
{
  "receiverId": "65a8c42e1234567890"
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Friend request sent",
  "data": {
    "id": "req_001",
    "sender": {
      "id": "user_001",
      "displayName": "Alice",
      "email": "alice@example.com",
      "avatarUrl": null
    },
    "receiver": {
      "id": "user_002",
      "displayName": "Bob",
      "email": "bob@example.com",
      "avatarUrl": null
    },
    "status": "PENDING",
    "createdAt": "2024-12-20T10:00:00Z"
  },
  "timestamp": "2024-12-20T10:00:00Z"
}
```

---

### 3) Get Pending Requests
**GET** `/api/friends/requests/pending`

**Response 200:**
```json
{
  "success": true,
  "message": "Pending requests retrieved",
  "data": [
    {
      "id": "req_001",
      "sender": {
        "id": "user_002",
        "displayName": "Bob",
        "email": "bob@example.com",
        "avatarUrl": null
      },
      "receiver": {
        "id": "user_001",
        "displayName": "Alice",
        "email": "alice@example.com",
        "avatarUrl": null
      },
      "status": "PENDING",
      "createdAt": "2024-12-20T10:00:00Z"
    }
  ],
  "timestamp": "2024-12-20T10:00:00Z"
}
```

---

### 4) Accept Request
**PUT** `/api/friends/requests/{id}/accept`

**Response 200:**
```json
{
  "success": true,
  "message": "Friend request accepted",
  "data": {
    "id": "user_002",
    "displayName": "Bob",
    "email": "bob@example.com",
    "avatarUrl": null,
    "since": "2024-12-20T10:00:00Z"
  },
  "timestamp": "2024-12-20T10:00:00Z"
}
```

---

### 5) Decline Request
**PUT** `/api/friends/requests/{id}/decline`

**Response 200:**
```json
{
  "success": true,
  "message": "Friend request declined",
  "data": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

---

### 6) Get Friend List
**GET** `/api/friends`

**Response 200:**
```json
{
  "success": true,
  "message": "Friend list retrieved",
  "data": [
    {
      "id": "user_002",
      "displayName": "Bob",
      "email": "bob@example.com",
      "avatarUrl": null,
      "since": "2024-12-20T10:00:00Z"
    }
  ],
  "timestamp": "2024-12-20T10:00:00Z"
}
```

---

### 7) Unfriend
**DELETE** `/api/friends/{friendId}`

**Response 200:**
```json
{
  "success": true,
  "message": "Unfriended successfully",
  "data": null,
  "timestamp": "2024-12-20T10:00:00Z"
}
```

---

## WebSocket Notifications

### Destination
- User queue: `/user/queue/friend-requests`

### Event Types
- `FRIEND_REQUEST_CREATED`
- `FRIEND_REQUEST_ACCEPTED`
- `FRIEND_REQUEST_DECLINED`

### Payload
```json
{
  "type": "FRIEND_REQUEST_CREATED",
  "request": {
    "id": "req_001",
    "sender": {
      "id": "user_001",
      "displayName": "Alice",
      "email": "alice@example.com",
      "avatarUrl": null
    },
    "receiver": {
      "id": "user_002",
      "displayName": "Bob",
      "email": "bob@example.com",
      "avatarUrl": null
    },
    "status": "PENDING",
    "createdAt": "2024-12-20T10:00:00Z"
  },
  "friend": null
}
```

For `FRIEND_REQUEST_ACCEPTED`, `friend` will contain the newly accepted friend info.
