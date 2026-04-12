# API Test - Chat Flow Complete

## 🎯 Test Flow Overview

```
1. Customer tạo CustomRequest
2. Customer publish CustomRequest
3. Artisan xem published requests
4. Artisan tạo Conversation
5. Customer xem conversations
6. Artisan gửi tin nhắn (REST API)
7. Customer gửi tin nhắn (REST API)
8. Get chat history
9. Mark messages as read
10. Get unread count
```

---

## 📝 Prerequisites

### 1. Login để lấy tokens

**Customer Login:**
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "customer@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "accountId": "customer-uuid-123"
  }
}
```

**Artisan Login:**
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "artisan@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "accountId": "artisan-uuid-456"
  }
}
```

**💡 Lưu tokens vào biến:**
- `CUSTOMER_TOKEN` = token của customer
- `ARTISAN_TOKEN` = token của artisan

---

## 🧪 Test Cases

### Test 1: Customer Tạo Custom Request

```http
POST http://localhost:8080/api/custom-requests
Authorization: Bearer {{CUSTOMER_TOKEN}}
Content-Type: application/json

{
  "requestType": "FREE_FORM",
  "title": "Tượng Đức Mẹ Maria kích thước lớn",
  "description": "Tôi muốn đặt làm tượng Đức Mẹ Maria cao 1m, chất liệu gỗ tốt, phong cách cổ điển",
  "budget": 5000000,
  "deadline": "2024-03-01T00:00:00",
  "referenceImages": ["https://example.com/image1.jpg"]
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Tạo yêu cầu thành công",
  "data": {
    "requestId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Tượng Đức Mẹ Maria kích thước lớn",
    "status": "DRAFT",
    "budget": 5000000,
    "createdAt": "2024-01-15T10:00:00"
  }
}
```

**💡 Lưu:** `REQUEST_ID` = requestId từ response

---

### Test 2: Customer Publish Request

```http
POST http://localhost:8080/api/custom-requests/{{REQUEST_ID}}/publish
Authorization: Bearer {{CUSTOMER_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Đã công khai yêu cầu",
  "data": {
    "requestId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "PUBLISHED",
    "publishedAt": "2024-01-15T10:05:00"
  }
}
```

---

### Test 3: Artisan Xem Published Requests

```http
GET http://localhost:8080/api/custom-requests/published?page=0&size=10
Authorization: Bearer {{ARTISAN_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "requestId": "550e8400-e29b-41d4-a716-446655440000",
        "title": "Tượng Đức Mẹ Maria kích thước lớn",
        "description": "Tôi muốn đặt làm tượng...",
        "budget": 5000000,
        "deadline": "2024-03-01T00:00:00",
        "status": "PUBLISHED",
        "customerName": "Nguyễn Văn A",
        "publishedAt": "2024-01-15T10:05:00"
      }
    ],
    "totalElements": 1
  }
}
```

---

### Test 4: Artisan Tạo Conversation (Quan trọng!)

```http
POST http://localhost:8080/api/conversations/start?requestId={{REQUEST_ID}}
Authorization: Bearer {{ARTISAN_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Bắt đầu hội thoại thành công",
  "data": {
    "conversationId": "660e8400-e29b-41d4-a716-446655440001",
    "requestId": "550e8400-e29b-41d4-a716-446655440000",
    "customerId": "customer-uuid-123",
    "customerName": "Nguyễn Văn A",
    "artisanId": "artisan-uuid-456",
    "artisanName": "Thợ Nguyễn",
    "createdAt": "2024-01-15T11:00:00"
  }
}
```

**💡 Lưu:** `CONVERSATION_ID` = conversationId từ response

---

### Test 5: Customer Xem Conversations

```http
GET http://localhost:8080/api/conversations/request/{{REQUEST_ID}}
Authorization: Bearer {{CUSTOMER_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Lấy danh sách hội thoại thành công",
  "data": [
    {
      "conversationId": "660e8400-e29b-41d4-a716-446655440001",
      "requestId": "550e8400-e29b-41d4-a716-446655440000",
      "artisanId": "artisan-uuid-456",
      "artisanName": "Thợ Nguyễn",
      "lastMessage": null,
      "unreadCount": 0,
      "createdAt": "2024-01-15T11:00:00"
    }
  ]
}
```

---

### Test 6: Artisan Gửi Tin Nhắn (REST API)

```http
POST http://localhost:8080/api/chat/send
Authorization: Bearer {{ARTISAN_TOKEN}}
Content-Type: application/json

{
  "conversationId": "{{CONVERSATION_ID}}",
  "content": "Chào anh, tôi đã xem yêu cầu của anh. Tôi có thể làm tượng này với giá 4,500,000 VND",
  "messageType": "TEXT"
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Gửi tin nhắn thành công",
  "data": {
    "messageId": "770e8400-e29b-41d4-a716-446655440002",
    "conversationId": "660e8400-e29b-41d4-a716-446655440001",
    "senderId": "artisan-uuid-456",
    "senderName": "Thợ Nguyễn",
    "content": "Chào anh, tôi đã xem yêu cầu của anh. Tôi có thể làm tượng này với giá 4,500,000 VND",
    "messageType": "TEXT",
    "sentAt": "2024-01-15T11:05:00",
    "isRead": false
  }
}
```

---

### Test 7: Customer Gửi Tin Nhắn (REST API)

```http
POST http://localhost:8080/api/chat/send
Authorization: Bearer {{CUSTOMER_TOKEN}}
Content-Type: application/json

{
  "conversationId": "{{CONVERSATION_ID}}",
  "content": "Giá này có bao gồm vận chuyển không? Thời gian hoàn thành bao lâu?",
  "messageType": "TEXT"
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Gửi tin nhắn thành công",
  "data": {
    "messageId": "880e8400-e29b-41d4-a716-446655440003",
    "conversationId": "660e8400-e29b-41d4-a716-446655440001",
    "senderId": "customer-uuid-123",
    "senderName": "Nguyễn Văn A",
    "content": "Giá này có bao gồm vận chuyển không? Thời gian hoàn thành bao lâu?",
    "messageType": "TEXT",
    "sentAt": "2024-01-15T11:10:00",
    "isRead": false
  }
}
```

---

### Test 8: Artisan Reply

```http
POST http://localhost:8080/api/chat/send
Authorization: Bearer {{ARTISAN_TOKEN}}
Content-Type: application/json

{
  "conversationId": "{{CONVERSATION_ID}}",
  "content": "Giá chưa bao gồm ship. Thời gian khoảng 30 ngày, chia 3 giai đoạn thanh toán",
  "messageType": "TEXT"
}
```

---

### Test 9: Get Chat History (với Pagination)

```http
GET http://localhost:8080/api/chat/conversation/{{CONVERSATION_ID}}/messages?page=0&size=50
Authorization: Bearer {{CUSTOMER_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Lấy tin nhắn thành công",
  "data": {
    "content": [
      {
        "messageId": "770e8400-e29b-41d4-a716-446655440002",
        "conversationId": "660e8400-e29b-41d4-a716-446655440001",
        "senderId": "artisan-uuid-456",
        "senderName": "Thợ Nguyễn",
        "content": "Chào anh, tôi đã xem yêu cầu của anh...",
        "messageType": "TEXT",
        "sentAt": "2024-01-15T11:05:00",
        "isRead": false
      },
      {
        "messageId": "880e8400-e29b-41d4-a716-446655440003",
        "conversationId": "660e8400-e29b-41d4-a716-446655440001",
        "senderId": "customer-uuid-123",
        "senderName": "Nguyễn Văn A",
        "content": "Giá này có bao gồm vận chuyển không?...",
        "messageType": "TEXT",
        "sentAt": "2024-01-15T11:10:00",
        "isRead": false
      }
    ],
    "totalElements": 3,
    "totalPages": 1,
    "size": 50,
    "number": 0
  }
}
```

---

### Test 10: Customer Mark Messages as Read

```http
POST http://localhost:8080/api/chat/conversation/{{CONVERSATION_ID}}/mark-read
Authorization: Bearer {{CUSTOMER_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Đánh dấu đã đọc thành công"
}
```

---

### Test 11: Get Unread Count (All Conversations)

```http
GET http://localhost:8080/api/chat/unread-count
Authorization: Bearer {{CUSTOMER_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Lấy số tin nhắn chưa đọc thành công",
  "data": 0
}
```

---

### Test 12: Get Unread Count (Specific Conversation)

```http
GET http://localhost:8080/api/chat/conversation/{{CONVERSATION_ID}}/unread-count
Authorization: Bearer {{ARTISAN_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Lấy số tin nhắn chưa đọc thành công",
  "data": 1
}
```

---

### Test 13: Get User Conversations (Customer)

```http
GET http://localhost:8080/api/chat/conversations
Authorization: Bearer {{CUSTOMER_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Lấy cuộc trò chuyện thành công",
  "data": [
    {
      "messageId": "880e8400-e29b-41d4-a716-446655440003",
      "conversationId": "660e8400-e29b-41d4-a716-446655440001",
      "senderId": "artisan-uuid-456",
      "senderName": "Thợ Nguyễn",
      "content": "Giá chưa bao gồm ship...",
      "messageType": "TEXT",
      "sentAt": "2024-01-15T11:15:00",
      "isRead": true,
      "otherParticipantId": "artisan-uuid-456",
      "otherParticipantName": "Thợ Nguyễn"
    }
  ]
}
```

---

## 📦 Postman Collection

### Import vào Postman

Tạo file `chat-flow-test.postman_collection.json`:

```json
{
  "info": {
    "name": "Chat Flow Test",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "BASE_URL",
      "value": "http://localhost:8080"
    },
    {
      "key": "CUSTOMER_TOKEN",
      "value": ""
    },
    {
      "key": "ARTISAN_TOKEN",
      "value": ""
    },
    {
      "key": "REQUEST_ID",
      "value": ""
    },
    {
      "key": "CONVERSATION_ID",
      "value": ""
    }
  ],
  "item": [
    {
      "name": "1. Customer Login",
      "event": [
        {
          "listen": "test",
          "script": {
            "exec": [
              "var jsonData = pm.response.json();",
              "pm.collectionVariables.set('CUSTOMER_TOKEN', jsonData.data.token);"
            ]
          }
        }
      ],
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"email\": \"customer@example.com\",\n  \"password\": \"password123\"\n}"
        },
        "url": {
          "raw": "{{BASE_URL}}/api/auth/login",
          "host": ["{{BASE_URL}}"],
          "path": ["api", "auth", "login"]
        }
      }
    },
    {
      "name": "2. Artisan Login",
      "event": [
        {
          "listen": "test",
          "script": {
            "exec": [
              "var jsonData = pm.response.json();",
              "pm.collectionVariables.set('ARTISAN_TOKEN', jsonData.data.token);"
            ]
          }
        }
      ],
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"email\": \"artisan@example.com\",\n  \"password\": \"password123\"\n}"
        },
        "url": {
          "raw": "{{BASE_URL}}/api/auth/login",
          "host": ["{{BASE_URL}}"],
          "path": ["api", "auth", "login"]
        }
      }
    },
    {
      "name": "3. Customer Create Request",
      "event": [
        {
          "listen": "test",
          "script": {
            "exec": [
              "var jsonData = pm.response.json();",
              "pm.collectionVariables.set('REQUEST_ID', jsonData.data.requestId);"
            ]
          }
        }
      ],
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{CUSTOMER_TOKEN}}"
          },
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"requestType\": \"FREE_FORM\",\n  \"title\": \"Tượng Đức Mẹ Maria kích thước lớn\",\n  \"description\": \"Tôi muốn đặt làm tượng Đức Mẹ Maria cao 1m\",\n  \"budget\": 5000000,\n  \"deadline\": \"2024-03-01T00:00:00\"\n}"
        },
        "url": {
          "raw": "{{BASE_URL}}/api/custom-requests",
          "host": ["{{BASE_URL}}"],
          "path": ["api", "custom-requests"]
        }
      }
    },
    {
      "name": "4. Customer Publish Request",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{CUSTOMER_TOKEN}}"
          }
        ],
        "url": {
          "raw": "{{BASE_URL}}/api/custom-requests/{{REQUEST_ID}}/publish",
          "host": ["{{BASE_URL}}"],
          "path": ["api", "custom-requests", "{{REQUEST_ID}}", "publish"]
        }
      }
    },
    {
      "name": "5. Artisan View Published Requests",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{ARTISAN_TOKEN}}"
          }
        ],
        "url": {
          "raw": "{{BASE_URL}}/api/custom-requests/published?page=0&size=10",
          "host": ["{{BASE_URL}}"],
          "path": ["api", "custom-requests", "published"],
          "query": [
            {"key": "page", "value": "0"},
            {"key": "size", "value": "10"}
          ]
        }
      }
    },
    {
      "name": "6. Artisan Create Conversation",
      "event": [
        {
          "listen": "test",
          "script": {
            "exec": [
              "var jsonData = pm.response.json();",
              "pm.collectionVariables.set('CONVERSATION_ID', jsonData.data.conversationId);"
            ]
          }
        }
      ],
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{ARTISAN_TOKEN}}"
          }
        ],
        "url": {
          "raw": "{{BASE_URL}}/api/conversations/start?requestId={{REQUEST_ID}}",
          "host": ["{{BASE_URL}}"],
          "path": ["api", "conversations", "start"],
          "query": [
            {"key": "requestId", "value": "{{REQUEST_ID}}"}
          ]
        }
      }
    },
    {
      "name": "7. Artisan Send Message",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{ARTISAN_TOKEN}}"
          },
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"conversationId\": \"{{CONVERSATION_ID}}\",\n  \"content\": \"Chào anh, tôi có thể làm tượng này với giá 4,500,000 VND\",\n  \"messageType\": \"TEXT\"\n}"
        },
        "url": {
          "raw": "{{BASE_URL}}/api/chat/send",
          "host": ["{{BASE_URL}}"],
          "path": ["api", "chat", "send"]
        }
      }
    },
    {
      "name": "8. Customer Send Message",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{CUSTOMER_TOKEN}}"
          },
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"conversationId\": \"{{CONVERSATION_ID}}\",\n  \"content\": \"Giá này có bao gồm vận chuyển không?\",\n  \"messageType\": \"TEXT\"\n}"
        },
        "url": {
          "raw": "{{BASE_URL}}/api/chat/send",
          "host": ["{{BASE_URL}}"],
          "path": ["api", "chat", "send"]
        }
      }
    },
    {
      "name": "9. Get Chat History",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{CUSTOMER_TOKEN}}"
          }
        ],
        "url": {
          "raw": "{{BASE_URL}}/api/chat/conversation/{{CONVERSATION_ID}}/messages?page=0&size=50",
          "host": ["{{BASE_URL}}"],
          "path": ["api", "chat", "conversation", "{{CONVERSATION_ID}}", "messages"],
          "query": [
            {"key": "page", "value": "0"},
            {"key": "size", "value": "50"}
          ]
        }
      }
    },
    {
      "name": "10. Mark Messages as Read",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{CUSTOMER_TOKEN}}"
          }
        ],
        "url": {
          "raw": "{{BASE_URL}}/api/chat/conversation/{{CONVERSATION_ID}}/mark-read",
          "host": ["{{BASE_URL}}"],
          "path": ["api", "chat", "conversation", "{{CONVERSATION_ID}}", "mark-read"]
        }
      }
    }
  ]
}
```

---

## ✅ Test Checklist

- [ ] Customer login thành công
- [ ] Artisan login thành công
- [ ] Customer tạo request thành công
- [ ] Customer publish request thành công
- [ ] Artisan xem được published requests
- [ ] Artisan tạo conversation thành công
- [ ] Customer xem được conversations
- [ ] Artisan gửi tin nhắn thành công
- [ ] Customer gửi tin nhắn thành công
- [ ] Get chat history trả về đúng messages
- [ ] Mark as read hoạt động đúng
- [ ] Unread count chính xác

---

## 🐛 Common Issues

| Issue | Solution |
|-------|----------|
| 401 Unauthorized | Check JWT token còn hạn không |
| 404 Not Found | Check REQUEST_ID và CONVERSATION_ID đúng chưa |
| 403 Forbidden | Check user có quyền truy cập không |
| Empty conversations | Đảm bảo đã publish request trước |
| No messages | Đảm bảo đã gửi tin nhắn qua API |

---

Chạy tuần tự từ Test 1 → Test 13 để test toàn bộ luồng chat!
