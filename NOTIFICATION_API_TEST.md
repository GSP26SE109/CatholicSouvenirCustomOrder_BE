# Notification API Test Guide

## 🔔 Notifications cần Real-time

Dựa vào luồng nghiệp vụ, các notifications sau CẦN real-time:

### 1. **NEW_REQUEST** - Customer publish request
- **Recipient**: TẤT CẢ Artisans
- **Priority**: HIGH
- **Action**: View request details
- **Real-time**: ✅ Broadcast to all artisans

### 2. **NEW_CONVERSATION** - Artisan tạo conversation
- **Recipient**: Customer
- **Priority**: NORMAL
- **Action**: View conversation
- **Real-time**: ✅ Notify customer instantly

### 3. **NEW_MESSAGE** - Tin nhắn mới
- **Recipient**: Other participant
- **Priority**: NORMAL
- **Action**: Open chat
- **Real-time**: ✅ Already handled by chat WebSocket

### 4. **ARTISAN_SELECTED** - Customer chọn artisan
- **Recipient**: Selected Artisan
- **Priority**: HIGH
- **Action**: Create order
- **Real-time**: ✅ Notify artisan instantly

### 5. **ORDER_CREATED** - Order được tạo
- **Recipient**: Customer
- **Priority**: HIGH
- **Action**: Pay stage 1
- **Real-time**: ✅ Notify customer

### 6. **PAYMENT_RECEIVED** - Thanh toán thành công
- **Recipient**: Artisan
- **Priority**: HIGH
- **Action**: Start work
- **Real-time**: ✅ Notify artisan

### 7. **STAGE_COMPLETED** - Stage hoàn thành
- **Recipient**: Customer
- **Priority**: HIGH
- **Action**: Pay next stage or view order
- **Real-time**: ✅ Notify customer

### 8. **ORDER_COMPLETED** - Order hoàn thành
- **Recipient**: Customer
- **Priority**: HIGH
- **Action**: View order
- **Real-time**: ✅ Notify customer

### 9. **SHIPMENT_CREATED** - Đơn hàng được gửi
- **Recipient**: Customer
- **Priority**: NORMAL
- **Action**: Track shipment
- **Real-time**: ✅ Notify customer

---

## 🧪 API Test Flow

### Setup: Login Users

```bash
# Customer Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "password123"
  }'

# Save token
CUSTOMER_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
CUSTOMER_ID="uuid-customer-123"

# Artisan Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "artisan@example.com",
    "password": "password123"
  }'

# Save token
ARTISAN_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
ARTISAN_ID="uuid-artisan-456"
```

---

## Test 1: Customer Publish Request → Notify All Artisans

### Step 1: Customer creates and publishes request

```bash
# Create request
curl -X POST http://localhost:8080/api/custom-requests \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "requestType": "FREE_FORM",
    "title": "Tượng Đức Mẹ Maria",
    "description": "Tượng cao 1m, gỗ tốt",
    "budget": 5000000,
    "deadline": "2024-03-01T00:00:00"
  }'

# Response: Get REQUEST_ID

# Publish request
curl -X POST http://localhost:8080/api/custom-requests/$REQUEST_ID/publish \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

### Step 2: All Artisans receive notification via WebSocket

**WebSocket Topic**: `/topic/notifications/{artisanId}`

**Notification received**:
```json
{
  "notificationId": "uuid-notif-1",
  "type": "NEW_REQUEST",
  "title": "Yêu cầu mới",
  "message": "Yêu cầu mới: Tượng Đức Mẹ Maria - Budget: 5,000,000 VND",
  "relatedEntityId": "uuid-request-1",
  "relatedEntityType": "CUSTOM_REQUEST",
  "isRead": false,
  "actionRequired": false,
  "priority": "HIGH",
  "createdAt": "2024-01-15T10:00:00"
}
```

### Step 3: Artisan checks notifications

```bash
# Get all notifications
curl -X GET "http://localhost:8080/api/notifications?page=0&size=20" \
  -H "Authorization: Bearer $ARTISAN_TOKEN"

# Get unread count
curl -X GET http://localhost:8080/api/notifications/unread-count \
  -H "Authorization: Bearer $ARTISAN_TOKEN"

# Response: { "code": 200, "data": 1 }
```

---

## Test 2: Artisan Creates Conversation → Notify Customer

```bash
# Artisan creates conversation
curl -X POST "http://localhost:8080/api/conversations/start?requestId=$REQUEST_ID" \
  -H "Authorization: Bearer $ARTISAN_TOKEN"

# Response: Get CONVERSATION_ID
```

**Customer receives notification via WebSocket**:
```json
{
  "notificationId": "uuid-notif-2",
  "type": "NEW_CONVERSATION",
  "title": "Nghệ nhân quan tâm",
  "message": "Nghệ nhân Thợ Nguyễn quan tâm đến yêu cầu của bạn",
  "relatedEntityId": "uuid-conversation-1",
  "relatedEntityType": "CONVERSATION",
  "isRead": false,
  "priority": "NORMAL",
  "createdAt": "2024-01-15T11:00:00"
}
```

---

## Test 3: Customer Selects Artisan → Notify Artisan

```bash
# Customer selects artisan
curl -X POST http://localhost:8080/api/custom-requests/$REQUEST_ID/select-artisan \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "artisanId": "'$ARTISAN_ID'"
  }'
```

**Artisan receives notification**:
```json
{
  "notificationId": "uuid-notif-3",
  "type": "ARTISAN_SELECTED",
  "title": "Bạn đã được chọn!",
  "message": "Nguyễn Văn A đã chọn bạn cho yêu cầu của họ",
  "relatedEntityId": "uuid-request-1",
  "relatedEntityType": "CUSTOM_REQUEST",
  "isRead": false,
  "actionRequired": true,
  "actionType": "CREATE_ORDER",
  "priority": "HIGH",
  "createdAt": "2024-01-15T12:00:00"
}
```

---

## Test 4: Artisan Creates Order → Notify Customer

```bash
# Artisan creates order with stages
curl -X POST http://localhost:8080/api/custom-orders \
  -H "Authorization: Bearer $ARTISAN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customRequestId": "'$REQUEST_ID'",
    "totalPrice": 4500000,
    "estimatedCompletionDate": "2024-02-15T00:00:00",
    "stages": [
      {
        "stageName": "Thiết kế 3D",
        "description": "Tạo mô hình 3D",
        "price": 500000,
        "estimatedDays": 5,
        "stageOrder": 1
      },
      {
        "stageName": "Gia công",
        "price": 2000000,
        "estimatedDays": 10,
        "stageOrder": 2
      },
      {
        "stageName": "Hoàn thiện",
        "price": 2000000,
        "estimatedDays": 15,
        "stageOrder": 3
      }
    ]
  }'

# Response: Get ORDER_ID
```

**Customer receives notification**:
```json
{
  "notificationId": "uuid-notif-4",
  "type": "ORDER_CREATED",
  "title": "Đơn hàng đã tạo",
  "message": "Đơn hàng của bạn đã được tạo với 3 giai đoạn. Tổng: 4,500,000 VND. Vui lòng thanh toán Giai đoạn 1.",
  "relatedEntityId": "uuid-order-1",
  "relatedEntityType": "CUSTOM_ORDER",
  "isRead": false,
  "actionRequired": true,
  "actionType": "PAY_STAGE",
  "priority": "HIGH",
  "createdAt": "2024-01-15T13:00:00"
}
```

---

## Test 5: Customer Pays Stage → Notify Artisan

```bash
# Customer initiates payment for stage 1
curl -X POST http://localhost:8080/api/stages/$STAGE_ID/payment/initiate \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethod": "VNPAY",
    "returnUrl": "http://localhost:3000/payment/success"
  }'

# After payment callback success...
```

**Artisan receives notification**:
```json
{
  "notificationId": "uuid-notif-5",
  "type": "PAYMENT_RECEIVED",
  "title": "Đã nhận thanh toán",
  "message": "Đã nhận thanh toán cho Thiết kế 3D: 500,000 VND. Bạn có thể bắt đầu làm việc.",
  "relatedEntityId": "uuid-stage-1",
  "relatedEntityType": "STAGE",
  "isRead": false,
  "actionRequired": false,
  "actionType": "COMPLETE_STAGE",
  "priority": "HIGH",
  "createdAt": "2024-01-15T14:00:00"
}
```

---

## Test 6: Artisan Completes Stage → Notify Customer

```bash
# Artisan completes stage
curl -X POST http://localhost:8080/api/stages/$STAGE_ID/complete \
  -H "Authorization: Bearer $ARTISAN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "completionNotes": "Đã hoàn thành thiết kế 3D",
    "proofImages": ["https://example.com/proof1.jpg"]
  }'
```

**Customer receives notification**:
```json
{
  "notificationId": "uuid-notif-6",
  "type": "STAGE_COMPLETED",
  "title": "Giai đoạn hoàn thành",
  "message": "Thiết kế 3D hoàn thành! Vui lòng thanh toán Gia công (2,000,000 VND) để tiếp tục.",
  "relatedEntityId": "uuid-stage-1",
  "relatedEntityType": "STAGE",
  "isRead": false,
  "actionRequired": true,
  "actionType": "PAY_STAGE",
  "priority": "HIGH",
  "createdAt": "2024-01-20T10:00:00"
}
```

---

## Test 7: Mark Notification as Read

```bash
# Mark single notification as read
curl -X POST http://localhost:8080/api/notifications/$NOTIFICATION_ID/read \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

# Mark all as read
curl -X POST http://localhost:8080/api/notifications/read-all \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

# Check unread count again
curl -X GET http://localhost:8080/api/notifications/unread-count \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

# Response: { "code": 200, "data": 0 }
```

---

## 🔌 WebSocket Connection Test

### JavaScript Test

```javascript
// Connect to notification WebSocket
const userId = 'uuid-customer-123';
const token = 'your-jwt-token';

const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect(
  { Authorization: `Bearer ${token}` },
  () => {
    console.log('✅ Connected to notification WebSocket');
    
    // Subscribe to user's notifications
    stompClient.subscribe(`/topic/notifications/${userId}`, (message) => {
      const notification = JSON.parse(message.body);
      console.log('🔔 New notification:', notification);
      
      // Show browser notification
      if (Notification.permission === 'granted') {
        new Notification(notification.title, {
          body: notification.message,
          icon: '/logo.png'
        });
      }
      
      // Update UI
      updateNotificationBadge();
      addNotificationToList(notification);
    });
  },
  (error) => {
    console.error('❌ WebSocket error:', error);
  }
);
```

---

## 📊 Notification Summary

| Event | Recipient | Type | Priority | Action Required |
|-------|-----------|------|----------|-----------------|
| Customer publish request | All Artisans | NEW_REQUEST | HIGH | No |
| Artisan create conversation | Customer | NEW_CONVERSATION | NORMAL | No |
| New message | Recipient | NEW_MESSAGE | NORMAL | No |
| Customer select artisan | Artisan | ARTISAN_SELECTED | HIGH | Yes (Create Order) |
| Artisan create order | Customer | ORDER_CREATED | HIGH | Yes (Pay Stage 1) |
| Customer pay stage | Artisan | PAYMENT_RECEIVED | HIGH | No |
| Artisan complete stage | Customer | STAGE_COMPLETED | HIGH | Yes (Pay Next/View) |
| All stages done | Customer | ORDER_COMPLETED | HIGH | No |
| Shipment created | Customer | SHIPMENT_CREATED | NORMAL | No |

---

## ✅ Test Checklist

- [ ] Customer publish request → All artisans receive notification
- [ ] Artisan create conversation → Customer receives notification
- [ ] Customer select artisan → Artisan receives notification
- [ ] Artisan create order → Customer receives notification
- [ ] Customer pay stage → Artisan receives notification
- [ ] Artisan complete stage → Customer receives notification
- [ ] WebSocket connection works
- [ ] Unread count updates correctly
- [ ] Mark as read works
- [ ] Browser notifications show (if permission granted)

---

Tất cả notifications đã sẵn sàng cho real-time!
