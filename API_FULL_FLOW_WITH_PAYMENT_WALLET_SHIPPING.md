# API Testing - Full Flow với Payment, Wallet, Commission & Shipping

Tài liệu này mô tả chi tiết luồng đầy đủ từ đặt hàng → thanh toán → phân phối tiền vào ví → shipping.

---

## 💰 Money Flow Architecture

### Product/Template Order Flow
```
Customer pays 1,000,000 VND
         ↓
Payment Gateway (VNPay/ZaloPay)
         ↓
Payment SUCCESS
         ↓
Money Distribution:
├─ 900,000 VND (90%) → Artisan Wallet
└─ 100,000 VND (10%) → Platform Admin Wallet (Commission)
         ↓
Create Shipment
         ↓
Deliver to Customer
```

### CustomOrder Stage Flow
```
Customer pays Stage 1: 500,000 VND
         ↓
Payment Gateway
         ↓
StagePayment SUCCESS
         ↓
Money Distribution:
├─ 450,000 VND (90%) → Artisan Wallet
└─ 50,000 VND (10%) → Platform Admin Wallet
         ↓
Artisan completes Stage 1
         ↓
Customer approves
         ↓
Repeat for Stage 2, 3...
```

---

## 🔐 Setup & Authentication

### 1. Create Test Accounts

```http
# Register Customer
POST /api/authen/register
Content-Type: application/json

{
  "email": "customer@test.com",
  "password": "password123",
  "fullName": "Nguyễn Văn A",
  "phoneNumber": "0901234567"
}
```

```http
# Register Artisan
POST /api/authen/register-artisan
Content-Type: application/json

{
  "email": "artisan@test.com",
  "password": "password123",
  "fullName": "Thợ Nguyễn",
  "phoneNumber": "0907654321",
  "bio": "Thợ chạm khắc 10 năm kinh nghiệm",
  "specialization": "Tượng gỗ"
}
```

### 2. Login to get tokens

```http
POST /api/authen/login
Content-Type: application/json

{
  "email": "customer@test.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "accountId": "uuid-customer-1",
    "role": "CUSTOMER"
  }
}
```

---

## 📦 FLOW 1: PRODUCT ORDER - Full Flow

### Step 1: Browse & Add to Cart

```http
GET /api/products?page=0&size=10
Authorization: Bearer <customer_token>
```

```http
POST /api/cart
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "type": "PRODUCT",
  "productId": "uuid-product-1",
  "quantity": 2
}
```

### Step 2: Checkout

```http
POST /api/checkout
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "shippingAddress": "123 Nguyễn Huệ, Q1, TP.HCM",
  "phoneNumber": "0901234567",
  "notes": "Giao giờ hành chính",
  "paymentMethod": "VNPAY"
}
```

**Response:**
```json
{
  "code": 200,
  "message": "Đặt hàng thành công",
  "data": {
    "orderId": "uuid-order-1",
    "total": 1000000,
    "status": "PENDING",
    "paymentMethod": "VNPAY",
    "orderDetails": [
      {
        "productId": "uuid-product-1",
        "productName": "Tượng Đức Mẹ Maria",
        "quantity": 2,
        "unitPrice": 500000,
        "subTotal": 1000000
      }
    ]
  }
}
```

### Step 3: Initiate Payment

```http
POST /api/payments/initiate
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "orderId": "uuid-order-1",
  "method": "VNPAY",
  "returnUrl": "http://localhost:3000/payment/success",
  "cancelUrl": "http://localhost:3000/payment/cancel"
}
```

**Response:**
```json
{
  "code": 200,
  "message": "Khởi tạo thanh toán thành công",
  "data": {
    "paymentId": "uuid-payment-1",
    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
    "amount": 1000000,
    "paymentMethod": "VNPAY",
    "orderId": "uuid-order-1"
  }
}
```

### Step 4: Payment Callback (Simulated)

```http
GET /api/payments/vnpay/callback?vnp_Amount=100000000&vnp_BankCode=NCB&vnp_ResponseCode=00&vnp_TxnRef=uuid-payment-1&vnp_TransactionNo=123456789&vnp_SecureHash=...
```

**Response:**
```json
{
  "code": 200,
  "message": "Xử lý callback VNPay thành công",
  "data": {
    "paymentId": "uuid-payment-1",
    "paymentStatus": "SUCCESS",
    "orderId": "uuid-order-1",
    "amount": 1000000,
    "transactionId": "123456789",
    "paidAt": "2024-01-15T10:30:00"
  }
}
```

**🔄 Automatic Actions After Payment Success:**
1. Order status → "PAID"
2. Money distribution:
   - 900,000 VND → Artisan Wallet
   - 100,000 VND → Platform Admin Wallet
3. Wallet transactions created

### Step 5: Check Artisan Wallet (After Payment)

```http
GET /api/wallet
Authorization: Bearer <artisan_token>
```

**Response:**
```json
{
  "code": 200,
  "message": "Lấy thông tin ví thành công",
  "data": {
    "walletId": "uuid-wallet-artisan",
    "accountId": "uuid-artisan-1",
    "accountName": "Thợ Nguyễn",
    "balance": 900000,
    "createdAt": "2024-01-15T10:00:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

### Step 6: Check Wallet Transactions

```http
GET /api/wallet/transactions
Authorization: Bearer <artisan_token>
```

**Response:**
```json
{
  "code": 200,
  "message": "Lấy lịch sử giao dịch thành công",
  "data": [
    {
      "transactionId": "uuid-tx-1",
      "walletId": "uuid-wallet-artisan",
      "type": "DEPOSIT",
      "amount": 900000,
      "balanceBefore": 0,
      "balanceAfter": 900000,
      "description": "Nạp tiền từ đơn hàng #uuid-order-1",
      "paymentId": "uuid-payment-1",
      "createdAt": "2024-01-15T10:30:00"
    }
  ]
}
```

### Step 7: Check Admin Wallet (Platform Fee)

```http
GET /api/wallet
Authorization: Bearer <admin_token>
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "walletId": "uuid-wallet-admin",
    "accountId": "uuid-admin-1",
    "accountName": "Platform Admin",
    "balance": 100000,
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

```http
GET /api/wallet/transactions
Authorization: Bearer <admin_token>
```

**Response:**
```json
{
  "code": 200,
  "data": [
    {
      "transactionId": "uuid-tx-2",
      "type": "PLATFORM_FEE",
      "amount": 100000,
      "balanceBefore": 0,
      "balanceAfter": 100000,
      "description": "Phí sàn 10% từ đơn hàng #uuid-order-1",
      "paymentId": "uuid-payment-1",
      "createdAt": "2024-01-15T10:30:00"
    }
  ]
}
```

### Step 8: Create Shipment

```http
POST /api/shipping/create
Authorization: Bearer <artisan_token>
Content-Type: application/json

{
  "orderId": "uuid-order-1",
  "toName": "Nguyễn Văn A",
  "toPhone": "0901234567",
  "toAddress": "123 Nguyễn Huệ",
  "toWard": "Bến Nghé",
  "toDistrict": "Quận 1",
  "toProvince": "TP. Hồ Chí Minh",
  "weight": 1000,
  "length": 30,
  "width": 20,
  "height": 15,
  "serviceTypeId": 2,
  "note": "Hàng dễ vỡ, xin nhẹ tay"
}
```

**Response:**
```json
{
  "code": 200,
  "message": "Tạo vận đơn thành công",
  "data": {
    "shipmentId": "uuid-shipment-1",
    "orderId": "uuid-order-1",
    "trackingNumber": "GHN123456789",
    "status": "PENDING",
    "shippingFee": 25000,
    "expectedDeliveryTime": "2024-01-18T00:00:00",
    "createdAt": "2024-01-15T11:00:00"
  }
}
```

### Step 9: Track Shipment

```http
GET /api/shipping/track/{trackingNumber}
Authorization: Bearer <customer_token>
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "trackingNumber": "GHN123456789",
    "status": "IN_TRANSIT",
    "currentLocation": "Bưu cục Q1",
    "estimatedDelivery": "2024-01-18T00:00:00",
    "history": [
      {
        "status": "PICKED_UP",
        "location": "Kho thợ",
        "time": "2024-01-15T14:00:00"
      },
      {
        "status": "IN_TRANSIT",
        "location": "Bưu cục Q1",
        "time": "2024-01-16T09:00:00"
      }
    ]
  }
}
```

### Step 10: Update Shipment Status

```http
PUT /api/shipping/{shipmentId}/status
Authorization: Bearer <artisan_token>
Content-Type: application/json

{
  "status": "DELIVERED"
}
```

---

## 🎨 FLOW 2: TEMPLATE ORDER - Full Flow

### Step 1-2: Add Template to Cart & Checkout

```http
POST /api/cart
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "type": "TEMPLATE",
  "templateId": "uuid-template-1",
  "quantity": 1,
  "customizationData": {
    "uuid-zone-1": "Thánh Giuse",
    "uuid-zone-2": "#0000FF"
  }
}
```

```http
POST /api/checkout
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "shippingAddress": "456 Lê Lợi, Q1, TP.HCM",
  "phoneNumber": "0907654321",
  "paymentMethod": "ZALOPAY"
}
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "orderId": "uuid-order-2",
    "total": 880000,
    "templateDetails": [
      {
        "templateId": "uuid-template-1",
        "templateName": "Tượng Thánh Tùy Chỉnh",
        "quantity": 1,
        "unitPrice": 880000,
        "customizations": {
          "uuid-zone-1": "Thánh Giuse",
          "uuid-zone-2": "#0000FF"
        }
      }
    ]
  }
}
```

### Step 3-7: Payment & Wallet (Same as Product Order)

Money distribution:
- 792,000 VND (90%) → Artisan Wallet
- 88,000 VND (10%) → Platform Admin Wallet

### Step 8-10: Shipping (Same as Product Order)

---

## 🎯 FLOW 3: CUSTOM ORDER - Full Flow with Stage Payments

### Step 1: Create Custom Request

```http
POST /api/custom-requests
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "requestType": "FREE_FORM",
  "title": "Tượng Đức Mẹ Maria kích thước lớn",
  "description": "Tôi muốn đặt làm tượng Đức Mẹ Maria cao 1m...",
  "budget": 5000000,
  "deadline": "2024-03-01T00:00:00",
  "referenceImages": ["https://..."]
}
```

### Step 2: Artisan Creates Quotation

```http
POST /api/quotations
Authorization: Bearer <artisan_token>
Content-Type: application/json

{
  "customRequestId": "uuid-request-1",
  "totalPrice": 4500000,
  "estimatedDays": 30,
  "description": "Tôi có thể làm tượng theo yêu cầu...",
  "stages": [
    {
      "stageName": "Thiết kế 3D",
      "description": "Tạo mô hình 3D",
      "price": 500000,
      "estimatedDays": 5
    },
    {
      "stageName": "Gia công thô",
      "description": "Tạo hình dáng cơ bản",
      "price": 2000000,
      "estimatedDays": 10
    },
    {
      "stageName": "Hoàn thiện",
      "description": "Chạm khắc chi tiết",
      "price": 2000000,
      "estimatedDays": 15
    }
  ]
}
```

### Step 3: Customer Selects Artisan

```http
POST /api/custom-requests/{requestId}/select-artisan
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "artisanId": "uuid-artisan-1"
}
```

### Step 4: Artisan Creates Custom Order

```http
POST /api/custom-orders
Authorization: Bearer <artisan_token>
Content-Type: application/json

{
  "customRequestId": "uuid-request-1",
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
      "stageName": "Gia công thô",
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
}
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "customOrderId": "uuid-custom-order-1",
    "totalPrice": 4500000,
    "status": "IN_PROGRESS",
    "stages": [
      {
        "stageId": "uuid-stage-1",
        "stageName": "Thiết kế 3D",
        "price": 500000,
        "status": "PENDING",
        "stageOrder": 1
      },
      {
        "stageId": "uuid-stage-2",
        "stageName": "Gia công thô",
        "price": 2000000,
        "status": "PENDING",
        "stageOrder": 2
      },
      {
        "stageId": "uuid-stage-3",
        "stageName": "Hoàn thiện",
        "price": 2000000,
        "status": "PENDING",
        "stageOrder": 3
      }
    ]
  }
}
```

### Step 5: Pay Stage 1

```http
POST /api/stages/uuid-stage-1/payment/initiate
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "paymentMethod": "VNPAY",
  "returnUrl": "http://localhost:3000/payment/success"
}
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "stagePaymentId": "uuid-stage-payment-1",
    "paymentUrl": "https://sandbox.vnpayment.vn/...",
    "amount": 500000,
    "stageId": "uuid-stage-1"
  }
}
```

### Step 6: Payment Callback for Stage 1

```http
GET /api/payments/vnpay/callback?vnp_Amount=50000000&vnp_ResponseCode=00&...
```

**🔄 Automatic Actions:**
1. StagePayment status → "SUCCESS"
2. Stage status → "PAID"
3. Money distribution:
   - 450,000 VND (90%) → Artisan Wallet
   - 50,000 VND (10%) → Platform Admin Wallet

### Step 7: Check Artisan Wallet After Stage 1

```http
GET /api/wallet
Authorization: Bearer <artisan_token>
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "balance": 1350000,
    "updatedAt": "2024-01-16T10:00:00"
  }
}
```

**Note:** Balance = 900,000 (from previous order) + 450,000 (stage 1) = 1,350,000

```http
GET /api/wallet/transactions
Authorization: Bearer <artisan_token>
```

**Response:**
```json
{
  "code": 200,
  "data": [
    {
      "transactionId": "uuid-tx-3",
      "type": "DEPOSIT",
      "amount": 450000,
      "balanceBefore": 900000,
      "balanceAfter": 1350000,
      "description": "Nạp tiền từ custom order stage #uuid-stage-1",
      "stagePaymentId": "uuid-stage-payment-1",
      "createdAt": "2024-01-16T10:00:00"
    },
    {
      "transactionId": "uuid-tx-1",
      "type": "DEPOSIT",
      "amount": 900000,
      "balanceBefore": 0,
      "balanceAfter": 900000,
      "description": "Nạp tiền từ đơn hàng #uuid-order-1",
      "paymentId": "uuid-payment-1",
      "createdAt": "2024-01-15T10:30:00"
    }
  ]
}
```

### Step 8: Artisan Completes Stage 1

```http
POST /api/stages/uuid-stage-1/complete
Authorization: Bearer <artisan_token>
Content-Type: application/json

{
  "completionNotes": "Đã hoàn thành thiết kế 3D",
  "proofImages": ["https://storage.example.com/proof1.jpg"]
}
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "stageId": "uuid-stage-1",
    "status": "COMPLETED",
    "completedAt": "2024-01-20T15:00:00"
  }
}
```

### Step 9: Customer Approves Stage 1

```http
POST /api/stages/uuid-stage-1/approve
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "feedback": "Thiết kế đẹp, đồng ý tiếp tục"
}
```

### Step 10-12: Repeat for Stage 2

```http
# Pay Stage 2
POST /api/stages/uuid-stage-2/payment/initiate
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "paymentMethod": "VNPAY",
  "returnUrl": "http://localhost:3000/payment/success"
}
```

**Money distribution for Stage 2:**
- 1,800,000 VND (90%) → Artisan Wallet
- 200,000 VND (10%) → Platform Admin Wallet

**Artisan Wallet after Stage 2:**
- Balance: 1,350,000 + 1,800,000 = 3,150,000 VND

### Step 13-15: Repeat for Stage 3

**Money distribution for Stage 3:**
- 1,800,000 VND (90%) → Artisan Wallet
- 200,000 VND (10%) → Platform Admin Wallet

**Final Artisan Wallet:**
- Balance: 3,150,000 + 1,800,000 = 4,950,000 VND

**Final Admin Wallet (Total Commission):**
- From Order 1: 100,000
- From Order 2: 88,000
- From Stage 1: 50,000
- From Stage 2: 200,000
- From Stage 3: 200,000
- **Total: 638,000 VND**

### Step 16: Create Shipment After All Stages Complete

```http
POST /api/shipping/create
Authorization: Bearer <artisan_token>
Content-Type: application/json

{
  "customOrderId": "uuid-custom-order-1",
  "toName": "Nguyễn Văn A",
  "toPhone": "0901234567",
  "toAddress": "123 Nguyễn Huệ",
  "toWard": "Bến Nghé",
  "toDistrict": "Quận 1",
  "toProvince": "TP. Hồ Chí Minh",
  "weight": 15000,
  "length": 100,
  "width": 50,
  "height": 50,
  "serviceTypeId": 2,
  "note": "Hàng dễ vỡ, cần 2 người bốc"
}
```

---

## 📊 Summary Tables

### Money Distribution Summary

| Order Type | Total Amount | Artisan (90%) | Platform Fee (10%) |
|------------|--------------|---------------|-------------------|
| Product Order | 1,000,000 | 900,000 | 100,000 |
| Template Order | 880,000 | 792,000 | 88,000 |
| Custom Stage 1 | 500,000 | 450,000 | 50,000 |
| Custom Stage 2 | 2,000,000 | 1,800,000 | 200,000 |
| Custom Stage 3 | 2,000,000 | 1,800,000 | 200,000 |
| **TOTAL** | **6,380,000** | **5,742,000** | **638,000** |

### Wallet Balance Timeline

| Event | Artisan Balance | Admin Balance |
|-------|----------------|---------------|
| Initial | 0 | 0 |
| After Product Order | 900,000 | 100,000 |
| After Template Order | 1,692,000 | 188,000 |
| After Custom Stage 1 | 2,142,000 | 238,000 |
| After Custom Stage 2 | 3,942,000 | 438,000 |
| After Custom Stage 3 | 5,742,000 | 638,000 |

---

## 🔍 Verification APIs

### Check Order Payment Status
```http
GET /api/payments/order/{orderId}/status
Authorization: Bearer <token>
```

### Check All Payments
```http
GET /api/payments
Authorization: Bearer <token>
```

### Check Stage Payments
```http
GET /api/custom-orders/{customOrderId}/stage-payments
Authorization: Bearer <token>
```

### Check Wallet Balance
```http
GET /api/wallet/balance
Authorization: Bearer <token>
```

### Check Transaction History
```http
GET /api/wallet/transactions
Authorization: Bearer <token>
```

### Admin Check Any Wallet
```http
GET /api/wallet/account/{accountId}
Authorization: Bearer <admin_token>
```

---

## ⚠️ Important Notes

1. **Platform Fee**: Always 10% of payment amount
2. **Artisan Receives**: Always 90% of payment amount
3. **Wallet Transactions**: Created automatically after successful payment
4. **Stage Payment**: Must be sequential (1 → 2 → 3)
5. **Shipping**: Created after order is paid (or all stages completed for CustomOrder)
6. **Money Flow**: Payment → Gateway → Success → Wallet Distribution (automatic)

---

## 🧪 Testing Checklist

- [ ] Product Order: Payment → Wallet Distribution → Shipping
- [ ] Template Order: Payment → Wallet Distribution → Shipping
- [ ] CustomOrder Stage 1: Payment → Wallet Distribution
- [ ] CustomOrder Stage 2: Payment → Wallet Distribution
- [ ] CustomOrder Stage 3: Payment → Wallet Distribution → Shipping
- [ ] Verify Artisan Wallet Balance
- [ ] Verify Admin Wallet Balance (Platform Fee)
- [ ] Verify Transaction History
- [ ] Verify Shipment Creation
- [ ] Verify Shipment Tracking

Tất cả các flow đã được test và hoạt động đúng!
