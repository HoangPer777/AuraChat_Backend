# Call Module - Flow & Endpoints for Frontend

## 1. **Quy Trình Gọi (Call Flow)**

```
┌─────────┐                                    ┌─────────┐
│ User A  │                                    │ User B  │
└────┬────┘                                    └────┬────┘
     │                                              │
     │ 1. Initiate Call (offer)                     │
     ├─────── POST /app/call/offer ───────────────>│
     │        {receiverId, type, sdp}              │
     │                                              │
     │ 2. Show Calling... UI                       │ 2. Show Incoming Call
     │                                              │
     │<────── STOMP /user/userA/queue/call ────────┤ 3. Send answer
     │        CallOfferDto                         ├─── POST /app/call/answer ──>
     │                                              │     {callId, sdp}
     │ 4. Accept received                          │
     │    Show call UI                             │ 4. Show call UI
     │                                              │
     │<────── STOMP /user/userA/queue/call ────────┤
     │        CallAnswerDto                        │
     │                                              │
     │ 5. Exchange ICE candidates                  │
     ├─────── POST /app/call/ice-candidate ──────> │
     │                                              │
     │<────── STOMP /user/userA/queue/call ────────┤
     │        IceCandidateDto                      │
     │                                              │
     │    (Both sides connect WebRTC)              │
     │    Media streams flowing...                 │
     │                                              │
     │ 6. End Call                                 │
     ├─────── POST /app/call/end ────────────────> │
     │        {callId}                             │
     │                                              │
     │<────── STOMP /user/userA/queue/call ────────┤
     │        CallResponse (COMPLETED)             │
     │                                              │
```

---

## 2. **WebSocket Setup (Frontend)**

### **2.1 Kết Nối STOMP**
```javascript
import StompJs from '@stomp/stompjs';

const client = new StompJs.Client({
  brokerURL: 'ws://localhost:8080/ws',
  connectHeaders: {
    Authorization: `Bearer ${jwtToken}`,
    login: userId // Optional, nhưng recommended
  },
  debug: (str) => console.log(str),
  reconnectDelay: 5000,
  heartbeatIncoming: 4000,
  heartbeatOutgoing: 4000,
  onConnect: onConnected,
  onDisconnect: onDisconnected,
  onStompError: onError,
  onWebSocketError: onWebSocketError
});

client.activate();

function onConnected() {
  console.log('Connected to WebSocket');
  
  // Subscribe to receive call notifications
  client.subscribe('/user/queue/call', (message) => {
    const payload = JSON.parse(message.body);
    handleCallMessage(payload);
  });
}

function handleCallMessage(message) {
  // message có thể là:
  // - CallOfferDto (incoming call)
  // - CallAnswerDto (call accepted)
  // - IceCandidateDto (ICE candidate)
  // - CallResponse (call ended/declined)
  
  if (message.sdp && !message.candidate) {
    // Nếu có sdp nhưng không có candidate → CallOfferDto hoặc CallAnswerDto
    if (message.type === 'VIDEO' || message.type === 'AUDIO') {
      handleIncomingOffer(message); // CallOfferDto
    } else {
      handleIncomingAnswer(message); // CallAnswerDto
    }
  } else if (message.candidate) {
    handleIceCandidate(message); // IceCandidateDto
  } else if (message.status) {
    handleCallResponse(message); // CallResponse
  }
}
```

---

## 3. **Endpoints Chi Tiết**

### **3.1 Bắt Đầu Gọi (Initiate Call)**

**Endpoint:** `POST /app/call/offer` (STOMP)

**Headers:**
```
Authorization: Bearer {jwtToken}
```

**Request Body:**
```json
{
  "receiverId": "userB",
  "type": "VIDEO",              
  "conversationId": "conv123",
  "sdp": "v=0\r\no=- 123456 123456 IN IP4 127.0.0.1\r\n..."
}
```

**Response (via STOMP /user/userB/queue/call):**
```json
{
  "callId": "call_abc123xyz",
  "callerId": "userA",
  "receiverId": "userB",
  "type": "VIDEO",
  "sdp": "v=0\r\no=- 123456 123456 IN IP4 127.0.0.1\r\n...",
  "conversationId": "conv123",
  "createdAt": "2026-05-15T10:30:00Z"
}
```

**Frontend Code:**
```javascript
async function initiateCall(receiverId, callType, conversationId, offerSdp) {
  const payload = {
    receiverId,
    type: callType, // 'VIDEO' or 'AUDIO'
    conversationId,
    sdp: offerSdp
  };
  
  client.publish({
    destination: '/app/call/offer',
    body: JSON.stringify(payload)
  });
  
  // Show calling UI
  showCallingUI(receiverId);
}
```

---

### **3.2 Chấp Nhận Gọi (Accept Call)**

**Endpoint:** `POST /app/call/answer` (STOMP)

**Headers:**
```
Authorization: Bearer {jwtToken}
```

**Request Body:**
```json
{
  "callId": "call_abc123xyz",
  "callerId": "userA",
  "receiverId": "userB",
  "sdp": "v=0\r\no=- 654321 654321 IN IP4 127.0.0.2\r\n..."
}
```

**Response (via STOMP /user/userA/queue/call):**
```json
{
  "callId": "call_abc123xyz",
  "callerId": "userA",
  "receiverId": "userB",
  "sdp": "v=0\r\no=- 654321 654321 IN IP4 127.0.0.2\r\n..."
}
```

**Frontend Code:**
```javascript
function acceptCall(callId, callerId, receiverId, answerSdp) {
  const payload = {
    callId,
    callerId,
    receiverId,
    sdp: answerSdp
  };
  
  client.publish({
    destination: '/app/call/answer',
    body: JSON.stringify(payload)
  });
  
  // Show active call UI
  showActiveCallUI();
}

// Trigger khi user click Accept
document.getElementById('accept-btn').addEventListener('click', () => {
  // Tạo answer SDP từ WebRTC
  peerConnection.createAnswer().then(answer => {
    peerConnection.setLocalDescription(answer);
    acceptCall(currentCall.callId, currentCall.callerId, userId, answer.sdp);
  });
});
```

---

### **3.3 Từ Chối Gọi (Decline Call)**

**Endpoint:** `POST /app/call/end` (STOMP) với status RINGING

**Headers:**
```
Authorization: Bearer {jwtToken}
```

**Request Body:**
```json
{
  "callId": "call_abc123xyz"
}
```

**Response (via STOMP):**
```json
{
  "callId": "call_abc123xyz",
  "status": "DECLINED",
  "message": "Call declined",
  "durationSeconds": 0,
  "endedAt": "2026-05-15T10:30:30Z"
}
```

**Frontend Code:**
```javascript
function declineCall(callId) {
  const payload = { callId };
  
  client.publish({
    destination: '/app/call/end',
    body: JSON.stringify(payload)
  });
  
  hideCallUI();
}

document.getElementById('decline-btn').addEventListener('click', () => {
  declineCall(currentCall.callId);
});
```

---

### **3.4 Gửi ICE Candidates**

**Endpoint:** `POST /app/call/ice-candidate` (STOMP)

**Request Body:**
```json
{
  "callId": "call_abc123xyz",
  "senderId": "userA",
  "receiverId": "userB",
  "candidate": "candidate:842350619 1 udp 1677729535 192.168.1.100 54321 typ srflx raddr 192.168.1.100 rport 54321 generation 0 ufrag EsIR network-cost 999",
  "sdpMid": "0",
  "sdpMLineIndex": 0
}
```

**Response (via STOMP /user/userB/queue/call):**
```json
{
  "callId": "call_abc123xyz",
  "senderId": "userA",
  "receiverId": "userB",
  "candidate": "candidate:842350619...",
  "sdpMid": "0",
  "sdpMLineIndex": 0
}
```

**Frontend Code:**
```javascript
// When WebRTC generates ICE candidates
peerConnection.onicecandidate = (event) => {
  if (event.candidate) {
    const candidate = {
      callId: currentCall.callId,
      senderId: userId,
      receiverId: currentCall.receiverId || currentCall.callerId,
      candidate: event.candidate.candidate,
      sdpMid: event.candidate.sdpMid,
      sdpMLineIndex: event.candidate.sdpMLineIndex
    };
    
    client.publish({
      destination: '/app/call/ice-candidate',
      body: JSON.stringify(candidate)
    });
  }
};

// Add received ICE candidates to connection
function handleIceCandidate(iceMessage) {
  const candidate = new RTCIceCandidate({
    candidate: iceMessage.candidate,
    sdpMid: iceMessage.sdpMid,
    sdpMLineIndex: iceMessage.sdpMLineIndex
  });
  
  peerConnection.addIceCandidate(candidate)
    .catch(e => console.error('Error adding ICE candidate:', e));
}
```

---

### **3.5 Kết Thúc Gọi (End Call)**

**Endpoint:** `POST /app/call/end` (STOMP)

**Request Body:**
```json
{
  "callId": "call_abc123xyz"
}
```

**Response (via STOMP):**
```json
{
  "callId": "call_abc123xyz",
  "status": "COMPLETED",
  "message": "Call ended",
  "durationSeconds": 120,
  "endedAt": "2026-05-15T10:32:00Z"
}
```

**Frontend Code:**
```javascript
function endCall(callId) {
  const payload = { callId };
  
  client.publish({
    destination: '/app/call/end',
    body: JSON.stringify(payload)
  });
  
  // Clean up WebRTC
  peerConnection.close();
  hideCallUI();
  showCallHistory();
}

// Cleanup
function handleCallEnded(response) {
  console.log(`Call ended. Duration: ${response.durationSeconds}s`);
  
  // Close peer connection
  peerConnection.close();
  
  // Show call ended UI
  showCallEndedUI(response);
  
  // Refresh call history
  setTimeout(() => fetchCallHistory(), 1000);
}
```

---

### **3.6 Lấy Lịch Sử Gọi (Get Call History) - REST API**

**Endpoint:** `GET /api/calls/history?page=0&size=50`

**Headers:**
```
Authorization: Bearer {jwtToken}
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "log_123",
      "conversationId": "conv123",
      "callerId": "userA",
      "receiverId": "userB",
      "type": "VIDEO",
      "status": "COMPLETED",
      "startedAt": "2026-05-15T10:30:00Z",
      "endedAt": "2026-05-15T10:32:00Z",
      "durationSeconds": 120,
      "createdAt": "2026-05-15T10:30:00Z"
    },
    {
      "id": "log_124",
      "conversationId": "conv124",
      "callerId": "userB",
      "receiverId": "userA",
      "type": "AUDIO",
      "status": "DECLINED",
      "startedAt": "2026-05-15T09:30:00Z",
      "endedAt": "2026-05-15T09:30:15Z",
      "durationSeconds": 0,
      "createdAt": "2026-05-15T09:30:00Z"
    }
  ],
  "message": "Call history retrieved",
  "timestamp": "2026-05-15T10:35:00Z"
}
```

**Frontend Code:**
```javascript
async function fetchCallHistory(page = 0, size = 50) {
  const response = await fetch(`/api/calls/history?page=${page}&size=${size}`, {
    headers: {
      'Authorization': `Bearer ${jwtToken}`
    }
  });
  
  const result = await response.json();
  displayCallHistory(result.data);
}

function displayCallHistory(calls) {
  calls.forEach(call => {
    const direction = call.callerId === userId ? 'Outgoing' : 'Incoming';
    const status = call.status; // COMPLETED, DECLINED, MISSED
    const duration = call.durationSeconds > 0 
      ? `${Math.floor(call.durationSeconds / 60)}m ${call.durationSeconds % 60}s`
      : 'No duration';
    
    console.log(`${direction} ${call.type} call to ${call.receiverId}: ${status} (${duration})`);
  });
}
```

---

## 4. **Call Status (Trạng Thái Gọi)**

| Status | Ý Nghĩa | Khi Nào Xảy Ra |
|--------|---------|----------------|
| **RINGING** | Gọi đang lấy tín hiệu | User A gửi offer, B chưa accept/decline |
| **ACCEPTED** | Gọi đã được chấp nhận | User B accept call, media bắt đầu |
| **COMPLETED** | Gọi đã kết thúc bình thường | User A/B click end call |
| **DECLINED** | Người nhận từ chối | User B click decline hoặc timeout |
| **MISSED** | Gọi nhỡ (tự động 60s) | User B không phản hồi trong 60s |

---

## 5. **Error Handling**

**Validation Errors (400):**
```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Receiver ID must not be blank",
  "timestamp": "2026-05-15T10:30:00Z"
}
```

**Authorization Error (401):**
```json
{
  "errorCode": "INVALID_JWT",
  "message": "JWT token expired or invalid",
  "timestamp": "2026-05-15T10:30:00Z"
}
```

**Call Not Found (404):**
```json
{
  "errorCode": "CALL_NOT_FOUND",
  "message": "Call with ID call_abc123 not found",
  "timestamp": "2026-05-15T10:30:00Z"
}
```

---

## 6. **Frontend Implementation Checklist**

```javascript
// ✓ WebSocket connection with JWT auth
// ✓ Subscribe to /user/queue/call
// ✓ Initiate call → POST /app/call/offer
// ✓ Accept call → POST /app/call/answer
// ✓ Decline call → POST /app/call/end
// ✓ Send ICE candidates → POST /app/call/ice-candidate
// ✓ Handle incoming offers (show incoming call UI)
// ✓ Handle ICE candidates (add to peer connection)
// ✓ Handle call end (cleanup WebRTC)
// ✓ Fetch call history → GET /api/calls/history
// ✓ Error handling & reconnection logic
// ✓ Timeout handling (no answer in 60s)
```

---

## 7. **Complete Frontend Example**

```javascript
class CallManager {
  constructor(userId, jwtToken) {
    this.userId = userId;
    this.jwtToken = jwtToken;
    this.client = null;
    this.peerConnection = null;
    this.currentCall = null;
  }

  connect() {
    this.client = new StompJs.Client({
      brokerURL: 'ws://localhost:8080/ws',
      connectHeaders: {
        Authorization: `Bearer ${this.jwtToken}`
      },
      onConnect: () => this.onConnected(),
      onDisconnect: () => this.onDisconnected()
    });
    this.client.activate();
  }

  onConnected() {
    console.log('Connected to call service');
    this.client.subscribe('/user/queue/call', (msg) => {
      this.handleIncomingMessage(JSON.parse(msg.body));
    });
  }

  handleIncomingMessage(message) {
    if (message.type && (message.type === 'VIDEO' || message.type === 'AUDIO')) {
      // Incoming call offer
      this.showIncomingCallUI(message);
      this.currentCall = message;
    } else if (message.status) {
      // Call response or end
      this.handleCallResponse(message);
    } else if (message.candidate) {
      // ICE candidate
      this.addIceCandidate(message);
    }
  }

  initiateCall(receiverId, type) {
    this.peerConnection = new RTCPeerConnection();
    this.peerConnection.onicecandidate = (e) => this.sendIceCandidate(e);

    navigator.mediaDevices.getUserMedia({ video: true, audio: true })
      .then(stream => {
        stream.getTracks().forEach(track => {
          this.peerConnection.addTrack(track, stream);
        });

        return this.peerConnection.createOffer();
      })
      .then(offer => {
        this.peerConnection.setLocalDescription(offer);
        
        this.client.publish({
          destination: '/app/call/offer',
          body: JSON.stringify({
            receiverId,
            type,
            conversationId: 'conv123',
            sdp: offer.sdp
          })
        });
      });
  }

  acceptCall(callId, callerId, receiverId) {
    this.peerConnection = new RTCPeerConnection();
    this.peerConnection.onicecandidate = (e) => this.sendIceCandidate(e);

    navigator.mediaDevices.getUserMedia({ video: true, audio: true })
      .then(stream => {
        stream.getTracks().forEach(track => {
          this.peerConnection.addTrack(track, stream);
        });

        return this.peerConnection.createAnswer();
      })
      .then(answer => {
        this.peerConnection.setLocalDescription(answer);

        this.client.publish({
          destination: '/app/call/answer',
          body: JSON.stringify({
            callId,
            callerId,
            receiverId,
            sdp: answer.sdp
          })
        });
      });
  }

  sendIceCandidate(event) {
    if (event.candidate) {
      const receiverId = this.currentCall.receiverId || this.currentCall.callerId;
      
      this.client.publish({
        destination: '/app/call/ice-candidate',
        body: JSON.stringify({
          callId: this.currentCall.callId,
          senderId: this.userId,
          receiverId,
          candidate: event.candidate.candidate,
          sdpMid: event.candidate.sdpMid,
          sdpMLineIndex: event.candidate.sdpMLineIndex
        })
      });
    }
  }

  addIceCandidate(iceMessage) {
    if (this.peerConnection) {
      this.peerConnection.addIceCandidate(
        new RTCIceCandidate({
          candidate: iceMessage.candidate,
          sdpMid: iceMessage.sdpMid,
          sdpMLineIndex: iceMessage.sdpMLineIndex
        })
      );
    }
  }

  endCall(callId) {
    this.peerConnection?.close();
    this.client.publish({
      destination: '/app/call/end',
      body: JSON.stringify({ callId })
    });
  }
}

// Usage
const callManager = new CallManager('userA', jwtToken);
callManager.connect();

// Initiate call
document.getElementById('call-btn').addEventListener('click', () => {
  callManager.initiateCall('userB', 'VIDEO');
});

// Accept call
document.getElementById('accept-btn').addEventListener('click', () => {
  callManager.acceptCall(currentCall.callId, currentCall.callerId, userId);
});

// End call
document.getElementById('end-btn').addEventListener('click', () => {
  callManager.endCall(currentCall.callId);
});
```

---

## 8. **Backend Stack**

- **Framework:** Spring Boot 3.x
- **WebSocket:** STOMP over SockJS
- **Real-time State:** Redis (with 1-hour TTL)
- **Persistence:** MongoDB (call logs)
- **Authentication:** JWT via JwtChannelInterceptor
- **Scheduling:** ScheduledExecutorService (60s timeout)

---

## 9. **Testing**

### Unit Tests (22 passing)
- CallServiceTest: 15 tests (validation, history, authorization)
- CallWebSocketIntegrationTest: 7 tests (DTO serialization)

### Manual Testing
- Use `wscat` or Postman WebSocket client
- Test scenarios: initiate → accept → ICE → end
- Verify Redis state and MongoDB logs

---

## 10. **Notes**

- JWT token required in WebSocket `Authorization` header
- Call timeout: 60 seconds (auto-logs as MISSED)
- Each call assigned unique UUID (`call_*`)
- Real-time messaging via STOMP `/app/` endpoints
- User-specific subscriptions via `/user/{userId}/queue/call`
- Call duration calculated from startedAt to endedAt
