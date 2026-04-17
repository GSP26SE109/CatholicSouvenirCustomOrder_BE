# Request-Based Custom Order - API Test Scenario (CORRECTED)

## Prerequisites
- Backend running on `http://localhost:8080`
- Customer account with JWT token
- Artisan account with JWT token  
- VNPay sandbox credentials configured

---

## PHASE 1: Customer Creates & Publishes Custom Request

### 1.1. Customer Login
```http
POST http://localhost:8080/api/authen/login
Content-Type: application/json

{
  "email": "customer@example.com",
  "password": "password123"
}
```

**Save:** `CUSTOMER_TOKEN` from response

---

### 1.2. Create Custom Request (DRAFT status)
```http
POST http://localhost:8080/api/custom-requests
Authorization: Bearer {{CUSTOMER_TOKEN}}
Content-Type: application/json

{
  "title": "Custom Rosary with Gold Chain",
  "description": "I want a custom rosary with gold-plated chain and pearl beads. The rosary should have 59 beads made from natural pearls, with a gold-plated crucifix and center medal. I prefer a traditional design with high-quality materials.",
  "minBudget": 1500000,
  "maxBudget": 2500000,
  "generateAiImage": true
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Tạo yêu cầu thành công",
  "data": {
    "requestId": "uuid-here",
    "status": "DRAFT",
    "requestType": "REQUEST_BASED",
    "title": "Custom Rosary with Gold Chain",
    "aiConceptImageUrl": "https://...",
    "imageGenCount": 1
  }
}
```

**Save:** `REQUEST_ID` from response

---

### 1.3. Publish Request (Make it visible to artisans)
```http
POST http://localhost:8080/api/custom-requests/{{REQUEST_ID}}/publish
Authorization: Bearer {{CUSTOMER_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Đăng yêu cầu thành công",
  "data": {
    "requestId": "{{REQUEST_ID}}",
    "status": "OPEN"
  }
}
```

**Note:** All artisans will receive notification about this new request

---

## PHASE 2: Artisans View & Start Conversations

### 2.1. Artisan Login
```http
POST http://localhost:8080/api/authen/login
Content-Type: application/json

{
  "email": "artisan@example.com",
  "password": "password123"
}
```

**Save:** `ARTISAN_TOKEN` and `ARTISAN_ID` from response

---

### 2.2. Artisan Views Open Requests
```http
GET http://localhost:8080/api/custom-requests/open?page=0&size=10
Authorization: Bearer {{ARTISAN_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "requestId": "{{REQUEST_ID}}",
        "title": "Custom Rosary with Gold Chain",
        "description": "...",
        "status": "OPEN",
        "minBudget": 1500000,
        "maxBudget": 2500000
      }
    ]
  }
}
```

---

### 2.3. Artisan Views Request Detail
```http
GET http://localhost:8080/api/custom-requests/{{REQUEST_ID}}
Authorization: Bearer {{ARTISAN_TOKEN}}
```

---

### 2.4. Artisan Starts Conversation (Shows Interest)
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
    "conversationId": "conversation-uuid",
    "requestId": "{{REQUEST_ID}}",
    "artisanId": "{{ARTISAN_ID}}",
    "artisanName": "John Artisan",
    "customerId": "{{CUSTOMER_ID}}",
    "createdAt": "2026-04-17T..."
  }
}
```

**Save:** `CONVERSATION_ID`

**Note:** Customer will receive notification that artisan is interested

---

## PHASE 3: Real-time Chat & Price Negotiation

### 3.1. Connect to WebSocket (Both Customer & Artisan)

**WebSocket URL:** `ws://localhost:8080/ws`

**Connection Headers:**
```
Authorization: Bearer {{TOKEN}}
```

**Subscribe to conversation topic:**
```
SUBSCRIBE /topic/conversation/{{CONVERSATION_ID}}
```

---

### 3.2. Artisan Sends Initial Message
```
SEND /app/chat/send
Content-Type: application/json

{
  "conversationId": "{{CONVERSATION_ID}}",
  "content": "Xin chào! Tôi đã xem yêu cầu của bạn về chuỗi hạt mân côi. Tôi có thể làm được với giá 1,800,000 VND. Bạn có muốn thảo luận thêm không?",
  "messageType": "TEXT"
}
```

**Customer receives via WebSocket:**
```json
{
  "messageId": "msg-uuid",
  "conversationId": "{{CONVERSATION_ID}}",
  "senderId": "{{ARTISAN_ID}}",
  "senderName": "John Artisan",
  "content": "Xin chào! Tôi đã xem yêu cầu...",
  "messageType": "TEXT",
  "sentAt": "2026-04-17T..."
}
```

---

### 3.3. Customer Responds
```
SEND /app/chat/send
Content-Type: application/json

{
  "conversationId": "{{CONVERSATION_ID}}",
  "content": "Cảm ơn bạn! Giá 1,800,000 VND có bao gồm vận chuyển không? Và mất bao lâu để hoàn thành?",
  "messageType": "TEXT"
}
```

---

### 3.4. Artisan Sends Image (Portfolio/Sample)
```
SEND /app/chat/send
Content-Type: application/json

{
  "conversationId": "{{CONVERSATION_ID}}",
  "content": "Đây là một số mẫu tôi đã làm trước đây",
  "messageType": "IMAGE",
  "imageUrl": "https://example.com/my-previous-work.jpg"
}
```

---

### 3.5. Continue Negotiation
```
# Artisan
{
  "conversationId": "{{CONVERSATION_ID}}",
  "content": "Giá đã bao gồm vận chuyển. Tôi sẽ chia thành 3 giai đoạn: 30% đặt cọc, 40% khi hoàn thành lắp ráp, 30% khi hoàn thành. Tổng thời gian khoảng 30 ngày.",
  "messageType": "TEXT"
}

# Customer
{
  "conversationId": "{{CONVERSATION_ID}}",
  "content": "Được! Tôi đồng ý với giá và cách chia giai đoạn này. Vậy tôi chọn bạn nhé!",
  "messageType": "TEXT"
}
```

---

### 3.6. Get Chat History (REST API)
```http
GET http://localhost:8080/api/conversations/{{CONVERSATION_ID}}/messages?page=0&size=50
Authorization: Bearer {{CUSTOMER_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "messageId": "msg-1",
        "content": "Xin chào! Tôi đã xem yêu cầu...",
        "senderName": "John Artisan",
        "messageType": "TEXT",
        "sentAt": "2026-04-17T10:00:00"
      },
      {
        "messageId": "msg-2",
        "content": "Cảm ơn bạn! Giá 1,800,000...",
        "senderName": "Customer Name",
        "messageType": "TEXT",
        "sentAt": "2026-04-17T10:05:00"
      }
    ]
  }
}
```

---

### 3.7. Customer Views All Interested Artisans
```http
GET http://localhost:8080/api/conversations/request/{{REQUEST_ID}}
Authorization: Bearer {{CUSTOMER_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "data": [
    {
      "conversationId": "conv-1",
      "artisanId": "artisan-1",
      "artisanName": "John Artisan",
      "lastMessage": "Giá đã bao gồm vận chuyển...",
      "lastMessageAt": "2026-04-17T10:15:00",
      "unreadCount": 0
    },
    {
      "conversationId": "conv-2",
      "artisanId": "artisan-2",
      "artisanName": "Jane Artisan",
      "lastMessage": "Tôi có thể làm với giá 2,000,000",
      "lastMessageAt": "2026-04-17T10:10:00",
      "unreadCount": 2
    }
  ]
}
```

**Note:** Customer can chat with multiple artisans to compare prices and quality

---

## PHASE 4: Customer Selects Artisan

### 4.1. Customer Selects Artisan After Negotiation
```http
POST http://localhost:8080/api/custom-requests/{{REQUEST_ID}}/select-artisan
Authorization: Bearer {{CUSTOMER_TOKEN}}
Content-Type: application/json

{
  "artisanId": "{{ARTISAN_ID}}"
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Chọn nghệ nhân thành công",
  "data": {
    "requestId": "{{REQUEST_ID}}",
    "status": "ARTISAN_SELECTED",
    "selectedArtisan": {
      "artisanId": "{{ARTISAN_ID}}",
      "name": "John Artisan"
    }
  }
}
```

**Note:** 
- Artisan will receive notification that they were selected
- Other artisans will be notified that customer chose someone else
- Request status changes to ARTISAN_SELECTED

---

## PHASE 5: Artisan Creates Custom Order with Stages
```http
POST http://localhost:8080/api/custom-requests/{{REQUEST_ID}}/select-artisan
Authorization: Bearer {{CUSTOMER_TOKEN}}
Content-Type: application/json

{
  "artisanId": "{{ARTISAN_ID}}"
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Chọn nghệ nhân thành công",
  "data": {
    "requestId": "{{REQUEST_ID}}",
    "status": "ARTISAN_SELECTED",
    "selectedArtisan": {
      "artisanId": "{{ARTISAN_ID}}",
      "name": "John Artisan"
    }
  }
}
```

**Note:** Artisan will receive notification that they were selected

---

## PHASE 5: Artisan Creates Custom Order with Stages

### 5.1. Artisan Creates Custom Order
```http
POST http://localhost:8080/api/custom-orders
Authorization: Bearer {{ARTISAN_TOKEN}}
Content-Type: application/json

{
  "requestId": "{{REQUEST_ID}}",
  "totalPrice": 1800000,
  "stages": [
    {
      "name": "Deposit - Material Purchase",
      "description": "Initial deposit for purchasing gold chain and pearl beads",
      "paymentPercentage": 30,
      "estimatedDays": 5
    },
    {
      "name": "Progress Payment - Assembly",
      "description": "Payment after assembling the rosary structure",
      "paymentPercentage": 40,
      "estimatedDays": 15
    },
    {
      "name": "Final Payment - Completion",
      "description": "Final payment upon completion and quality check",
      "paymentPercentage": 30,
      "estimatedDays": 10
    }
  ],
  "shippingAddress": "123 Main St, District 1, HCMC"
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Tạo đơn hàng thành công",
  "data": {
    "customOrderId": "order-uuid",
    "status": "PENDING_PAYMENT",
    "totalPrice": 1800000,
    "stages": [
      {
        "stageId": "stage-1-uuid",
        "stageOrder": 1,
        "stageName": "Deposit - Material Purchase",
        "amount": 540000,
        "percentage": 30,
        "status": "PENDING",
        "canPay": true,
        "isPaid": false,
        "isCompleted": false
      },
      {
        "stageId": "stage-2-uuid",
        "stageOrder": 2,
        "stageName": "Progress Payment - Assembly",
        "amount": 720000,
        "percentage": 40,
        "status": "PENDING",
        "canPay": false,
        "isPaid": false,
        "isCompleted": false
      },
      {
        "stageId": "stage-3-uuid",
        "stageOrder": 3,
        "stageName": "Final Payment - Completion",
        "amount": 540000,
        "percentage": 30,
        "status": "PENDING",
        "canPay": false,
        "isPaid": false,
        "isCompleted": false
      }
    ]
  }
}
```

**Save:** 
- `CUSTOM_ORDER_ID`
- `STAGE_1_ID` (first stage)
- `STAGE_2_ID` (second stage)
- `STAGE_3_ID` (third stage)

**Note:** 
- Amounts are calculated automatically: totalPrice * paymentPercentage / 100
- Only Stage 1 has `canPay: true` initially
- Customer will receive notification about new order

---

## PHASE 6: Customer Pays Stage 1 (Deposit)

### 6.1. Customer Views Order Details
```http
GET http://localhost:8080/api/custom-orders/{{CUSTOM_ORDER_ID}}
Authorization: Bearer {{CUSTOMER_TOKEN}}
```

---

### 5.2. Initiate Payment for Stage 1
```http
POST http://localhost:8080/api/stage-payments/{{STAGE_1_ID}}/initiate
Authorization: Bearer {{CUSTOMER_TOKEN}}
Content-Type: application/json

{
  "paymentMethod": "VNPAY",
  "returnUrl": "http://localhost:3000/custom-order/payment-result"
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Khởi tạo thanh toán giai đoạn thành công",
  "data": {
    "paymentId": "payment-uuid",
    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
    "amount": 540000,
    "transactionId": "STAGE_{{STAGE_1_ID}}_1234567890"
  }
}
```

**Save:** `PAYMENT_URL_STAGE_1`

---

### 5.3. Customer Completes Payment on VNPay
1. Open `PAYMENT_URL_STAGE_1` in browser
2. Use VNPay sandbox test card:
   - Card Number: `9704198526191432198`
   - Name: `NGUYEN VAN A`
   - Issue Date: `07/15`
   - OTP: `123456`
3. Complete payment
4. VNPay redirects to backend: `/api/stage-payments/vnpay/return`
5. Backend processes payment and redirects to: `{{returnUrl}}?paymentId=xxx&status=SUCCESS`

---

### 5.4. Verify Stage 1 Payment Status
```http
GET http://localhost:8080/api/custom-orders/{{CUSTOM_ORDER_ID}}
Authorization: Bearer {{CUSTOMER_TOKEN}}
```

**Expected Response:**
```json
{
  "data": {
    "status": "IN_PROGRESS",
    "stages": [
      {
        "stageId": "{{STAGE_1_ID}}",
        "stageOrder": 1,
        "status": "PAID",
        "amount": 540000,
        "canPay": false,
        "isPaid": true,
        "isCompleted": false,
        "paidAt": "2026-04-17T..."
      },
      {
        "stageId": "{{STAGE_2_ID}}",
        "stageOrder": 2,
        "status": "PENDING",
        "canPay": false,
        "isPaid": false
      }
    ]
  }
}
```

---

### 5.5. Check Wallet Balances After Stage 1 Payment

**Artisan Wallet:**
```http
GET http://localhost:8080/api/wallets/balance
Authorization: Bearer {{ARTISAN_TOKEN}}
```

**Expected:** Balance increased by `486,000 VND` (90% of 540,000)

**Platform Admin Wallet:**
```http
GET http://localhost:8080/api/wallets/balance
Authorization: Bearer {{ADMIN_TOKEN}}
```

**Expected:** Balance increased by `54,000 VND` (10% of 540,000)

---

## PHASE 7: Artisan Completes Stage 1

### 7.1. Artisan Uploads Proof Image (Optional)
```http
POST http://localhost:8080/api/custom-order-stages/{{STAGE_1_ID}}/proof-image
Authorization: Bearer {{ARTISAN_TOKEN}}
Content-Type: application/json

{
  "imageUrl": "https://example.com/materials-purchased.jpg"
}
```

---

### 6.2. Artisan Marks Stage 1 as Complete
```http
POST http://localhost:8080/api/custom-order-stages/{{STAGE_1_ID}}/complete
Authorization: Bearer {{ARTISAN_TOKEN}}
Content-Type: application/json

{
  "completionImageUrl": "https://example.com/stage1-complete.jpg"
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Hoàn thành giai đoạn thành công",
  "data": {
    "stageId": "{{STAGE_1_ID}}",
    "stageOrder": 1,
    "status": "COMPLETED",
    "isCompleted": true,
    "completedAt": "2026-04-17T..."
  }
}
```

**Note:** Customer will receive notification about stage completion

---

### 6.3. Verify Stage 2 is Now Unlocked
```http
GET http://localhost:8080/api/custom-orders/{{CUSTOM_ORDER_ID}}
Authorization: Bearer {{CUSTOMER_TOKEN}}
```

**Expected Response:**
```json
{
  "data": {
    "stages": [
      {
        "stageOrder": 1,
        "status": "COMPLETED",
        "isCompleted": true,
        "canPay": false
      },
      {
        "stageOrder": 2,
        "status": "PENDING",
        "canPay": true,
        "isPaid": false
      }
    ]
  }
}
```

**Note:** Stage 2 now has `canPay: true`

---

## PHASE 8: Repeat for Stage 2

### 8.1. Customer Pays Stage 2
```http
POST http://localhost:8080/api/stage-payments/{{STAGE_2_ID}}/initiate
Authorization: Bearer {{CUSTOMER_TOKEN}}
Content-Type: application/json

{
  "paymentMethod": "VNPAY",
  "returnUrl": "http://localhost:3000/custom-order/payment-result"
}
```

### 7.2. Complete Payment on VNPay
(Same process as Stage 1)

### 7.3. Verify Stage 2 Payment
**Expected:** 
- Stage 2 status = PAID
- Amount = 720,000
- Artisan wallet: +648,000 (90%)
- Platform wallet: +72,000 (10%)

### 7.4. Artisan Completes Stage 2
```http
POST http://localhost:8080/api/custom-order-stages/{{STAGE_2_ID}}/complete
Authorization: Bearer {{ARTISAN_TOKEN}}
Content-Type: application/json

{
  "completionImageUrl": "https://example.com/stage2-complete.jpg"
}
```

### 7.5. Verify Stage 3 Unlocked
**Expected:** Stage 3 now has `canPay: true`

---

## PHASE 9: Final Stage (Stage 3)

### 9.1. Customer Pays Stage 3
```http
POST http://localhost:8080/api/stage-payments/{{STAGE_3_ID}}/initiate
Authorization: Bearer {{CUSTOMER_TOKEN}}
Content-Type: application/json

{
  "paymentMethod": "VNPAY",
  "returnUrl": "http://localhost:3000/custom-order/payment-result"
}
```

### 8.2. Complete Payment
**Expected:**
- Stage 3 status = PAID
- Amount = 540,000
- Artisan wallet: +486,000 (90%)
- Platform wallet: +54,000 (10%)

### 8.3. Artisan Completes Stage 3
```http
POST http://localhost:8080/api/custom-order-stages/{{STAGE_3_ID}}/complete
Authorization: Bearer {{ARTISAN_TOKEN}}
Content-Type: application/json

{
  "completionImageUrl": "https://example.com/final-product.jpg"
}
```

---

## PHASE 10: Verify Final State

### 10.1. Check Custom Order Status
```http
GET http://localhost:8080/api/custom-orders/{{CUSTOM_ORDER_ID}}
Authorization: Bearer {{CUSTOMER_TOKEN}}
```

**Expected:**
```json
{
  "data": {
    "customOrderId": "{{CUSTOM_ORDER_ID}}",
    "status": "COMPLETED",
    "totalPrice": 1800000,
    "stages": [
      {
        "stageOrder": 1,
        "status": "COMPLETED",
        "amount": 540000,
        "isPaid": true,
        "isCompleted": true
      },
      {
        "stageOrder": 2,
        "status": "COMPLETED",
        "amount": 720000,
        "isPaid": true,
        "isCompleted": true
      },
      {
        "stageOrder": 3,
        "status": "COMPLETED",
        "amount": 540000,
        "isPaid": true,
        "isCompleted": true
      }
    ]
  }
}
```

---

### 10.2. Check Total Wallet Balances

**Artisan Total Earnings:**
- Stage 1: 486,000 (90% of 540,000)
- Stage 2: 648,000 (90% of 720,000)
- Stage 3: 486,000 (90% of 540,000)
- **Total: 1,620,000 VND** (90% of 1,800,000)

**Platform Total Fee:**
- Stage 1: 54,000 (10%)
- Stage 2: 72,000 (10%)
- Stage 3: 54,000 (10%)
- **Total: 180,000 VND** (10% of 1,800,000)

---

## Edge Cases to Test

### Test 1: Cannot Pay Stage 2 Before Stage 1 is Completed
```http
POST http://localhost:8080/api/stage-payments/{{STAGE_2_ID}}/initiate
Authorization: Bearer {{CUSTOMER_TOKEN}}
```

**Expected:** `400 Bad Request` - "Giai đoạn trước phải được hoàn thành và thanh toán trước"

---

### Test 2: Cannot Complete Stage Before Payment
```http
POST http://localhost:8080/api/custom-order-stages/{{STAGE_1_ID}}/complete
Authorization: Bearer {{ARTISAN_TOKEN}}
```

**Expected:** `400 Bad Request` - "Giai đoạn phải được thanh toán trước khi hoàn thành"

---

### Test 3: Retry Payment (Cancel Old Pending Payment)
```http
# Initiate payment
POST http://localhost:8080/api/stage-payments/{{STAGE_1_ID}}/initiate

# Don't complete payment, initiate again
POST http://localhost:8080/api/stage-payments/{{STAGE_1_ID}}/initiate
```

**Expected:** Old pending payment cancelled, new payment created with new URL

---

### Test 4: Invalid Stage Percentages (Must Sum to 100%)
```http
POST http://localhost:8080/api/custom-orders
Authorization: Bearer {{ARTISAN_TOKEN}}
Content-Type: application/json

{
  "requestId": "{{REQUEST_ID}}",
  "totalPrice": 1000000,
  "stages": [
    {"name": "Stage 1", "paymentPercentage": 50, "estimatedDays": 10},
    {"name": "Stage 2", "paymentPercentage": 40, "estimatedDays": 10}
  ]
}
```

**Expected:** `400 Bad Request` - "Tổng phần trăm thanh toán phải bằng 100% (hiện tại: 90%)"

---

### Test 5: Cannot Select Artisan for Non-OPEN Request
```http
POST http://localhost:8080/api/custom-requests/{{REQUEST_ID}}/select-artisan
Authorization: Bearer {{CUSTOMER_TOKEN}}
Content-Type: application/json

{
  "artisanId": "{{ARTISAN_ID}}"
}
```

**Expected:** `400 Bad Request` if request status is not OPEN

---

### Test 6: Cannot Create Order if Not Selected Artisan
```http
POST http://localhost:8080/api/custom-orders
Authorization: Bearer {{DIFFERENT_ARTISAN_TOKEN}}
Content-Type: application/json

{
  "requestId": "{{REQUEST_ID}}",
  "totalPrice": 1000000,
  "stages": [...]
}
```

**Expected:** `403 Forbidden` - "Bạn không phải là nghệ nhân được chọn cho yêu cầu này"

---

## Success Criteria

✅ Customer can create REQUEST_BASED custom request (DRAFT)
✅ Customer can publish request (OPEN) → All artisans notified
✅ Artisans can view open requests
✅ Artisans can start conversations to show interest
✅ Real-time chat via WebSocket works for price negotiation
✅ Customer can chat with multiple artisans simultaneously
✅ Customer can view chat history and compare offers
✅ Customer can select artisan after negotiation
✅ Selected artisan can create custom order with stages
✅ Stage amounts calculated automatically from percentages
✅ Only first stage can be paid initially
✅ Payment distribution works (90% artisan, 10% platform)
✅ Completing stage unlocks next stage for payment
✅ All stages must be completed sequentially
✅ Final order status = COMPLETED after all stages done
✅ Wallet balances correct for artisan and platform
✅ Notifications sent at each step

---

## Flow Summary

```
1. Customer: Create Request (DRAFT)
2. Customer: Publish Request (OPEN) → Notify all artisans
3. Artisans: Start Conversations (Show Interest)
4. Customer & Artisans: Real-time Chat & Price Negotiation via WebSocket
5. Customer: Select Artisan (After comparing offers) → Status: ARTISAN_SELECTED
6. Artisan: Create Custom Order with Stages → Status: PENDING_PAYMENT
7. Customer: Pay Stage 1 → Stage 1: PAID
8. Artisan: Complete Stage 1 → Stage 1: COMPLETED, Stage 2: Unlocked
9. Customer: Pay Stage 2 → Stage 2: PAID
10. Artisan: Complete Stage 2 → Stage 2: COMPLETED, Stage 3: Unlocked
11. Customer: Pay Stage 3 → Stage 3: PAID
12. Artisan: Complete Stage 3 → Order: COMPLETED
```

---

## Notes

- Replace `{{VARIABLE}}` with actual values from previous responses
- VNPay sandbox URL valid for 15 minutes
- Check logs for payment distribution details
- Verify notifications sent to customer/artisan after each step
- WebSocket endpoint: `ws://localhost:8080/ws`
- Chat messages are real-time via WebSocket
- Customer can negotiate with multiple artisans before selecting one

## Prerequisites
- Backend running on `http://localhost:8080`
- Customer account with JWT token
- Artisan account with JWT token
- VNPay sandbox credentials configured

---

## PHASE 1: Customer Creates Custom Request

### 1.1. Customer Login
```http
POST http://localhost:8080/api/authen/login
Content-Type: application/json

{
  "email": "customer@example.com",
  "password": "password123"
}
```

**Save:** `CUSTOMER_TOKEN` from response

---

### 1.2. Create Custom Request (REQUEST_BASED)
```http
POST http://localhost:8080/api/custom-requests
Authorization: Bearer {{CUSTOMER_TOKEN}}
Content-Type: application/json

{
  "requestType": "REQUEST_BASED",
  "title": "Custom Rosary with Gold Chain",
  "description": "I want a custom rosary with gold-plated chain and pearl beads",
  "budget": 2000000,
  "deadline": "2026-06-01T00:00:00",
  "referenceImages": [
    "https://example.com/image1.jpg"
  ]
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Tạo yêu cầu thành công",
  "data": {
    "requestId": "uuid-here",
    "status": "PENDING",
    "requestType": "REQUEST_BASED"
  }
}
```

**Save:** `REQUEST_ID` from response

---

## PHASE 2: Artisan Responds & Gets Selected

### 2.1. Artisan Login
```http
POST http://localhost:8080/api/authen/login
Content-Type: application/json

{
  "email": "artisan@example.com",
  "password": "password123"
}
```

**Save:** `ARTISAN_TOKEN` from response

---

### 2.2. Artisan Views Available Requests
```http
GET http://localhost:8080/api/custom-requests?status=PENDING
Authorization: Bearer {{ARTISAN_TOKEN}}
```

---

### 2.3. Artisan Submits Quotation
```http
POST http://localhost:8080/api/custom-requests/{{REQUEST_ID}}/quotations
Authorization: Bearer {{ARTISAN_TOKEN}}
Content-Type: application/json

{
  "proposedPrice": 1800000,
  "estimatedDays": 30,
  "description": "I can create this rosary with premium materials",
  "sampleImages": [
    "https://example.com/my-work1.jpg"
  ]
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Gửi báo giá thành công"
}
```

---

### 2.4. Customer Selects Artisan
```http
POST http://localhost:8080/api/custom-requests/{{REQUEST_ID}}/select-artisan
Authorization: Bearer {{CUSTOMER_TOKEN}}
Content-Type: application/json

{
  "artisanId": "{{ARTISAN_ID}}"
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Chọn nghệ nhân thành công",
  "data": {
    "status": "ARTISAN_SELECTED"
  }
}
```

---

## PHASE 3: Artisan Creates Custom Order with Stages

### 3.1. Create Custom Order with Payment Stages
```http
POST http://localhost:8080/api/custom-orders
Authorization: Bearer {{ARTISAN_TOKEN}}
Content-Type: application/json

{
  "requestId": "{{REQUEST_ID}}",
  "totalPrice": 1800000,
  "stages": [
    {
      "name": "Deposit - Material Purchase",
      "description": "Initial deposit for purchasing materials",
      "paymentPercentage": 30,
      "estimatedDays": 5
    },
    {
      "name": "Progress Payment - Assembly",
      "description": "Payment after assembling the rosary",
      "paymentPercentage": 40,
      "estimatedDays": 15
    },
    {
      "name": "Final Payment - Completion",
      "description": "Final payment upon completion",
      "paymentPercentage": 30,
      "estimatedDays": 10
    }
  ],
  "shippingAddress": "123 Main St, District 1, HCMC"
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Tạo đơn hàng thành công",
  "data": {
    "customOrderId": "uuid-here",
    "status": "PENDING_PAYMENT",
    "totalPrice": 1800000,
    "stages": [
      {
        "stageId": "stage-1-uuid",
        "stageOrder": 1,
        "stageName": "Deposit - Material Purchase",
        "amount": 540000,
        "percentage": 30,
        "canPay": true,
        "isPaid": false
      },
      {
        "stageId": "stage-2-uuid",
        "stageOrder": 2,
        "stageName": "Progress Payment - Assembly",
        "amount": 720000,
        "percentage": 40,
        "canPay": false,
        "isPaid": false
      },
      {
        "stageId": "stage-3-uuid",
        "stageOrder": 3,
        "stageName": "Final Payment - Completion",
        "amount": 540000,
        "percentage": 30,
        "canPay": false,
        "isPaid": false
      }
    ]
  }
}
```

**Save:** 
- `CUSTOM_ORDER_ID`
- `STAGE_1_ID` (first stage)
- `STAGE_2_ID` (second stage)
- `STAGE_3_ID` (third stage)

---

## PHASE 4: Customer Pays Stage 1 (Deposit)

### 4.1. Initiate Payment for Stage 1
```http
POST http://localhost:8080/api/stage-payments/{{STAGE_1_ID}}/initiate
Authorization: Bearer {{CUSTOMER_TOKEN}}
Content-Type: application/json

{
  "paymentMethod": "VNPAY",
  "returnUrl": "http://localhost:3000/custom-order/payment-result"
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Khởi tạo thanh toán giai đoạn thành công",
  "data": {
    "paymentId": "payment-uuid",
    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
    "amount": 540000
  }
}
```

**Save:** `PAYMENT_URL_STAGE_1`

---

### 4.2. Customer Completes Payment on VNPay
1. Open `PAYMENT_URL_STAGE_1` in browser
2. Use VNPay sandbox test card:
   - Card Number: `9704198526191432198`
   - Name: `NGUYEN VAN A`
   - Issue Date: `07/15`
   - OTP: `123456`
3. Complete payment

---

### 4.3. Verify Stage 1 Payment Status
```http
GET http://localhost:8080/api/custom-orders/{{CUSTOM_ORDER_ID}}
Authorization: Bearer {{CUSTOMER_TOKEN}}
```

**Expected Response:**
```json
{
  "data": {
    "stages": [
      {
        "stageId": "{{STAGE_1_ID}}",
        "stageOrder": 1,
        "status": "PAID",
        "canPay": false,
        "isPaid": true,
        "paidAt": "2026-04-17T..."
      },
      {
        "stageId": "{{STAGE_2_ID}}",
        "stageOrder": 2,
        "status": "PENDING",
        "canPay": false,
        "isPaid": false
      }
    ]
  }
}
```

---

### 4.4. Check Wallet Balances
**Artisan Wallet:**
```http
GET http://localhost:8080/api/wallets/balance
Authorization: Bearer {{ARTISAN_TOKEN}}
```

**Expected:** Balance increased by `486,000` (90% of 540,000)

**Platform Admin Wallet:**
```http
GET http://localhost:8080/api/wallets/balance
Authorization: Bearer {{ADMIN_TOKEN}}
```

**Expected:** Balance increased by `54,000` (10% of 540,000)

---

## PHASE 5: Artisan Completes Stage 1

### 5.1. Artisan Uploads Proof Image
```http
POST http://localhost:8080/api/custom-order-stages/{{STAGE_1_ID}}/proof-image
Authorization: Bearer {{ARTISAN_TOKEN}}
Content-Type: application/json

{
  "imageUrl": "https://example.com/materials-purchased.jpg"
}
```

---

### 5.2. Artisan Marks Stage 1 as Complete
```http
POST http://localhost:8080/api/custom-order-stages/{{STAGE_1_ID}}/complete
Authorization: Bearer {{ARTISAN_TOKEN}}
Content-Type: application/json

{
  "completionImageUrl": "https://example.com/stage1-complete.jpg"
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Hoàn thành giai đoạn thành công",
  "data": {
    "stageId": "{{STAGE_1_ID}}",
    "status": "COMPLETED",
    "isCompleted": true
  }
}
```

---

### 5.3. Verify Stage 2 is Now Unlocked
```http
GET http://localhost:8080/api/custom-orders/{{CUSTOM_ORDER_ID}}
Authorization: Bearer {{CUSTOMER_TOKEN}}
```

**Expected Response:**
```json
{
  "data": {
    "stages": [
      {
        "stageOrder": 1,
        "status": "COMPLETED",
        "isCompleted": true
      },
      {
        "stageOrder": 2,
        "status": "PENDING",
        "canPay": true,
        "isPaid": false
      }
    ]
  }
}
```

---

## PHASE 6: Customer Pays Stage 2

### 6.1. Initiate Payment for Stage 2
```http
POST http://localhost:8080/api/stage-payments/{{STAGE_2_ID}}/initiate
Authorization: Bearer {{CUSTOMER_TOKEN}}
Content-Type: application/json

{
  "paymentMethod": "VNPAY",
  "returnUrl": "http://localhost:3000/custom-order/payment-result"
}
```

### 6.2. Complete Payment on VNPay
(Same process as Stage 1)

### 6.3. Verify Stage 2 Payment
```http
GET http://localhost:8080/api/custom-orders/{{CUSTOM_ORDER_ID}}
Authorization: Bearer {{CUSTOMER_TOKEN}}
```

**Expected:** Stage 2 status = PAID, amount = 720,000

---

## PHASE 7: Complete Remaining Stages

Repeat PHASE 5 & 6 for:
- Stage 2 completion → Unlock Stage 3
- Stage 3 payment → Final payment (540,000)
- Stage 3 completion → Order COMPLETED

---

## PHASE 8: Verify Final State

### 8.1. Check Custom Order Status
```http
GET http://localhost:8080/api/custom-orders/{{CUSTOM_ORDER_ID}}
Authorization: Bearer {{CUSTOMER_TOKEN}}
```

**Expected:**
```json
{
  "data": {
    "status": "COMPLETED",
    "totalPrice": 1800000,
    "stages": [
      {"stageOrder": 1, "status": "COMPLETED", "isPaid": true},
      {"stageOrder": 2, "status": "COMPLETED", "isPaid": true},
      {"stageOrder": 3, "status": "COMPLETED", "isPaid": true}
    ]
  }
}
```

---

### 8.2. Check Total Wallet Balances

**Artisan Total Earnings:**
- Stage 1: 486,000 (90% of 540,000)
- Stage 2: 648,000 (90% of 720,000)
- Stage 3: 486,000 (90% of 540,000)
- **Total: 1,620,000** (90% of 1,800,000)

**Platform Total Fee:**
- Stage 1: 54,000 (10%)
- Stage 2: 72,000 (10%)
- Stage 3: 54,000 (10%)
- **Total: 180,000** (10% of 1,800,000)

---

## Edge Cases to Test

### Test 1: Cannot Pay Stage 2 Before Stage 1 is Completed
```http
POST http://localhost:8080/api/stage-payments/{{STAGE_2_ID}}/initiate
Authorization: Bearer {{CUSTOMER_TOKEN}}
```

**Expected:** `400 Bad Request` - "Giai đoạn trước phải được hoàn thành và thanh toán trước"

---

### Test 2: Cannot Complete Stage Before Payment
```http
POST http://localhost:8080/api/custom-order-stages/{{STAGE_1_ID}}/complete
Authorization: Bearer {{ARTISAN_TOKEN}}
```

**Expected:** `400 Bad Request` - "Giai đoạn phải được thanh toán trước khi hoàn thành"

---

### Test 3: Retry Payment (Cancel Old Pending Payment)
```http
# Initiate payment
POST http://localhost:8080/api/stage-payments/{{STAGE_1_ID}}/initiate

# Don't complete payment, initiate again
POST http://localhost:8080/api/stage-payments/{{STAGE_1_ID}}/initiate
```

**Expected:** Old pending payment cancelled, new payment created

---

### Test 4: Invalid Stage Percentages
```http
POST http://localhost:8080/api/custom-orders
Authorization: Bearer {{ARTISAN_TOKEN}}
Content-Type: application/json

{
  "requestId": "{{REQUEST_ID}}",
  "totalPrice": 1000000,
  "stages": [
    {"name": "Stage 1", "paymentPercentage": 50},
    {"name": "Stage 2", "paymentPercentage": 40}
  ]
}
```

**Expected:** `400 Bad Request` - "Tổng phần trăm thanh toán phải bằng 100% (hiện tại: 90%)"

---

## Success Criteria

✅ Customer can create REQUEST_BASED custom request
✅ Artisan can submit quotation
✅ Customer can select artisan
✅ Artisan can create custom order with stages
✅ Stage amounts calculated automatically from percentages
✅ Only first stage can be paid initially
✅ Payment distribution works (90% artisan, 10% platform)
✅ Completing stage unlocks next stage for payment
✅ All stages must be completed sequentially
✅ Final order status = COMPLETED after all stages done
✅ Wallet balances correct for artisan and platform

---

## Notes

- Replace `{{VARIABLE}}` with actual values from previous responses
- VNPay sandbox URL valid for 15 minutes
- Check logs for payment distribution details
- Verify notifications sent to customer after each stage
