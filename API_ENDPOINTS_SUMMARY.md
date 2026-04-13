# API Endpoints Summary - Quick Reference

## 🔐 Authentication

### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}

Response:
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "accountId": "uuid-123",
    "role": "CUSTOMER" // or "ARTISAN"
  }
}
```

---

## 📝 Custom Request APIs

### 1. Create Request (Customer)
```http
POST /api/custom-requests
Authorization: Bearer {token}

{
  "requestType": "FREE_FORM",
  "title": "Tượng Đức Mẹ Maria",
  "description": "Chi tiết yêu cầu...",
  "budget": 5000000,
  "deadline": "2024-03-01T00:00:00"
}
```

### 2. Publish Request (Customer)
```http
POST /api/custom-requests/{requestId}/publish
Authorization: Bearer {token}
```

### 3. Get Published Requests (Artisan)
```http
GET /api/custom-requests/published?page=0&size=20
Authorization: Bearer {token}
```

---

## 💬 Conversation APIs

### 1. Create Conversation (Artisan)
```http
POST /api/conversations/start?requestId={requestId}
Authorization: Bearer {token}

Response:
{
  "code": 200,
  "data": {
    "conversationId": "uuid-conv-1",
    "requestId": "uuid-req-1",
    "customerId": "uuid-customer",
    "customerName": "Nguyễn Văn A",
    "artisanId": "uuid-artisan",
    "artisanName": "Thợ Nguyễn"
  }
}
```

### 2. Get Conversations for Request (Customer)
```http
GET /api/conversations/request/{requestId}
Authorization: Bearer {token}

Response:
{
  "code": 200,
  "data": [
    {
      "conversationId": "uuid-conv-1",
      "artisanId": "uuid-artisan",
      "artisanName": "Thợ Nguyễn",
      "createdAt": "2024-01-15T11:00:00"
    }
  ]
}
```

### 3. Get My Conversations (Artisan)
```http
GET /api/conversations/my-conversations
Authorization: Bearer {token}
```

---

## 💬 Chat APIs

### 1. Send Message (REST API)
```http
POST /api/chat/send
Authorization: Bearer {token}

{
  "conversationId": "uuid-conv-1",
  "content": "Xin chào!",
  "messageType": "TEXT"
}

Response:
{
  "code": 200,
  "data": {
    "messageId": "uuid-msg-1",
    "conversationId": "uuid-conv-1",
    "senderId": "uuid-user",
    "senderName": "Nguyễn Văn A",
    "content": "Xin chào!",
    "messageType": "TEXT",
    "sentAt": "2024-01-15T11:05:00",
    "isRead": false
  }
}
```

### 2. Get Chat History
```http
GET /api/chat/conversation/{conversationId}/messages?page=0&size=50
Authorization: Bearer {token}

Response:
{
  "code": 200,
  "data": {
    "content": [
      {
        "messageId": "uuid-msg-1",
        "senderId": "uuid-user",
        "senderName": "Nguyễn Văn A",
        "content": "Xin chào!",
        "sentAt": "2024-01-15T11:05:00",
        "isRead": true
      }
    ],
    "totalElements": 10,
    "totalPages": 1
  }
}
```

### 3. Mark Messages as Read
```http
POST /api/chat/conversation/{conversationId}/mark-read
Authorization: Bearer {token}
```

### 4. Get Unread Count
```http
GET /api/chat/unread-count
Authorization: Bearer {token}

Response:
{
  "code": 200,
  "data": 5
}
```

---

## 🔌 WebSocket

### Connection
```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect(
  { Authorization: `Bearer ${token}` },
  () => {
    // Subscribe to conversation
    stompClient.subscribe(`/topic/chat/${conversationId}`, (message) => {
      const chatMessage = JSON.parse(message.body);
      console.log('New message:', chatMessage);
    });
    
    // Send message
    stompClient.send(
      `/app/chat/${conversationId}`,
      {},
      JSON.stringify({
        conversationId: conversationId,
        content: 'Hello!',
        messageType: 'TEXT'
      })
    );
  }
);
```

---

## 📊 Response Format

### Success Response
```json
{
  "code": 200,
  "message": "Success message",
  "data": { ... }
}
```

### Error Response
```json
{
  "code": 400,
  "message": "Error message",
  "data": null
}
```

---

## 🎯 Complete Flow Example

### Customer Flow
```
1. POST /api/auth/login
   → Get token

2. POST /api/custom-requests
   → Create request
   → Get requestId

3. POST /api/custom-requests/{requestId}/publish
   → Publish request

4. GET /api/conversations/request/{requestId}
   → See interested artisans
   → Get conversationId

5. Connect WebSocket
   → Subscribe /topic/chat/{conversationId}

6. POST /api/chat/send
   → Send messages

7. GET /api/chat/conversation/{conversationId}/messages
   → Load chat history
```

### Artisan Flow
```
1. POST /api/auth/login
   → Get token

2. GET /api/custom-requests/published
   → Browse requests
   → Choose requestId

3. POST /api/conversations/start?requestId={requestId}
   → Create conversation
   → Get conversationId

4. Connect WebSocket
   → Subscribe /topic/chat/{conversationId}

5. POST /api/chat/send
   → Send messages

6. GET /api/chat/conversation/{conversationId}/messages
   → Load chat history
```

---

## 🔑 Important Notes

1. **Token**: Lưu token vào localStorage sau khi login
2. **ConversationId**: Lấy từ API response, không hardcode
3. **WebSocket**: Luôn check connection status trước khi send
4. **Pagination**: Default page=0, size=50
5. **MessageType**: Hiện tại chỉ support "TEXT"

---

## 📱 Mobile App (React Native)

Tương tự web nhưng dùng:
- `axios` cho REST API
- `stompjs` + `sockjs-client` cho WebSocket
- `AsyncStorage` thay vì `localStorage`

---

## 🐛 Error Codes

| Code | Meaning | Action |
|------|---------|--------|
| 200 | Success | Continue |
| 400 | Bad Request | Check request body |
| 401 | Unauthorized | Redirect to login |
| 403 | Forbidden | Check user role |
| 404 | Not Found | Check ID exists |
| 500 | Server Error | Retry or contact support |

---

## 🚀 Testing

### Postman
Import file: `chat-flow-test.postman_collection.json`

### cURL
```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'

# Create Request
curl -X POST http://localhost:8080/api/custom-requests \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"requestType":"FREE_FORM","title":"Test","description":"Test","budget":1000000,"deadline":"2024-03-01T00:00:00"}'
```

---

Tất cả endpoints đã sẵn sàng để Frontend integrate!
