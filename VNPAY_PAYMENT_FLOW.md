# VNPay Payment Flow - Luồng Thanh Toán

## 📋 Tổng Quan

VNPay sử dụng **2 callback riêng biệt** với mục đích khác nhau:

1. **Return URL** (`/vnpay/return`) - Redirect user về FE/Mobile
2. **IPN URL** (`/vnpay/ipn`) - Update DB (server-to-server)

---

## 🔄 Luồng Chi Tiết

```
┌─────────────┐
│   User      │
│ (Web/Mobile)│
└──────┬──────┘
       │
       │ 1. POST /api/payments/initiate
       │    { orderId, method: "VNPAY", returnUrl? }
       ▼
┌─────────────────────────────────────────────────────────┐
│  Backend (PaymentServiceImp)                            │
│  - Tạo Payment record (status: PENDING)                 │
│  - Gọi VNPayUtil.createPaymentUrl()                     │
│  - Return: { paymentUrl: "https://sandbox.vnpayment..." }│
└──────┬──────────────────────────────────────────────────┘
       │
       │ 2. Response: paymentUrl
       ▼
┌─────────────┐
│   User      │
│  Click URL  │
└──────┬──────┘
       │
       │ 3. Redirect to VNPay
       ▼
┌─────────────────────────────────────────────────────────┐
│  VNPay Payment Gateway                                  │
│  - User nhập OTP (sandbox: 123456)                     │
│  - User click "Thanh toán"                             │
└──────┬──────────────────────────────────────────────────┘
       │
       │ ⚡ VNPay thực hiện 2 callback ĐỒNG THỜI:
       │
       ├─────────────────────────────────────────────────┐
       │                                                 │
       │ 4a. RETURN URL (User Redirect)                 │ 4b. IPN URL (Server Callback)
       │     GET /api/payments/vnpay/return             │     GET /api/payments/vnpay/ipn
       │     ?vnp_ResponseCode=00                       │     ?vnp_ResponseCode=00
       │     &vnp_TxnRef=ORDER_xxx                      │     &vnp_TxnRef=ORDER_xxx
       │     &platform=web/mobile                       │     &vnp_SecureHash=...
       ▼                                                 ▼
┌──────────────────────────────────┐    ┌──────────────────────────────────┐
│ PaymentController                │    │ PaymentController                │
│ handleVNPayReturn()              │    │ handleVNPayIPN()                 │
│                                  │    │                                  │
│ ❌ KHÔNG update DB               │    │ ✅ UPDATE DB                     │
│ ✅ CHỈ redirect user             │    │ - Verify signature               │
│                                  │    │ - Update Payment status          │
│ if (platform == "mobile"):       │    │ - Update Order status            │
│   redirect to:                   │    │ - Save transaction ID            │
│   myapp://payment/result?...     │    │                                  │
│ else:                            │    │ Response:                        │
│   redirect to:                   │    │ {"RspCode":"00",                 │
│   https://fe.com/payment/result? │    │  "Message":"Confirm Success"}    │
└──────┬───────────────────────────┘    └──────────────────────────────────┘
       │                                                 │
       │ 5. User được redirect                          │ (VNPay nhận response)
       ▼                                                 │
┌─────────────────────────────────┐                     │
│  Frontend/Mobile App            │                     │
│  /payment/result                │                     │
│  ?orderId=xxx                   │                     │
│  &success=true                  │                     │
│  &code=00                       │                     │
│                                 │                     │
│  6. App có thể:                 │                     │
│  - Show success message         │◄────────────────────┘
│  - Poll GET /api/payments/      │  (DB đã được update)
│    order/{orderId}/latest       │
│  - Hiển thị order details       │
└─────────────────────────────────┘
```

---

## 🎯 Vai Trò Của Mỗi Endpoint

### 1️⃣ Return URL (`/vnpay/return`)
**Mục đích:** Redirect user về app sau khi thanh toán

**Đặc điểm:**
- ❌ **KHÔNG** update database
- ✅ **CHỈ** redirect user
- User thấy kết quả ngay lập tức
- Có thể bị user cancel/close trước khi load

**Code:**
```java
@GetMapping("/vnpay/return")
public void handleVNPayReturn(@RequestParam Map<String, String> params,
                               @RequestParam(required = false) String platform,
                               HttpServletResponse response) {
    String vnpResponseCode = params.get("vnp_ResponseCode");
    String txnRef = params.get("vnp_TxnRef");
    boolean isSuccess = "00".equals(vnpResponseCode);
    
    // Redirect về FE/Mobile
    if ("mobile".equalsIgnoreCase(platform)) {
        response.sendRedirect("myapp://payment/result?orderId=" + txnRef + "&success=" + isSuccess);
    } else {
        response.sendRedirect("https://your-fe.com/payment/result?orderId=" + txnRef + "&success=" + isSuccess);
    }
}
```

### 2️⃣ IPN URL (`/vnpay/ipn`)
**Mục đích:** VNPay gọi server-to-server để update DB

**Đặc điểm:**
- ✅ **UPDATE DATABASE** ở đây
- ✅ Verify signature
- ✅ Update Payment status
- ✅ Update Order status
- Đảm bảo chạy ngay cả khi user đóng browser
- VNPay sẽ retry nếu không nhận được response "00"

**Code:**
```java
@GetMapping("/vnpay/ipn")
public ResponseEntity<Map<String, String>> handleVNPayIPN(@RequestParam Map<String, String> params) {
    try {
        // IMPORTANT: Pass copy of params
        PaymentCallbackRequest request = PaymentCallbackRequest.builder()
                .params(new HashMap<>(params))
                .paymentGateway("VNPAY")
                .build();
        
        // ✅ UPDATE DB HERE
        paymentService.handlePaymentCallback(request);
        
        // VNPay expects this exact format
        Map<String, String> response = new HashMap<>();
        response.put("RspCode", "00");
        response.put("Message", "Confirm Success");
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        Map<String, String> response = new HashMap<>();
        response.put("RspCode", "99");
        response.put("Message", "Unknown error");
        return ResponseEntity.ok(response);
    }
}
```

---

## 🔐 Tại Sao Cần 2 Callback?

### Return URL
- **User experience**: User thấy kết quả ngay
- **Không đáng tin cậy**: User có thể đóng browser trước khi load
- **Có thể bị giả mạo**: Params có thể bị modify

### IPN URL
- **Đáng tin cậy**: Server-to-server, không qua user
- **Có signature verification**: Đảm bảo từ VNPay
- **Retry mechanism**: VNPay sẽ gọi lại nếu fail
- **Source of truth**: Đây là nguồn chính thức để update DB

---

## 📱 Cách Frontend/Mobile Sử Dụng

### Web Frontend
```javascript
// 1. Initiate payment
const response = await fetch('/api/payments/initiate', {
  method: 'POST',
  body: JSON.stringify({
    orderId: 'xxx',
    method: 'VNPAY',
    returnUrl: null  // Use default
  })
});

const { paymentUrl } = await response.json();

// 2. Redirect to VNPay
window.location.href = paymentUrl;

// 3. User sẽ được redirect về:
// https://your-fe.com/payment/result?orderId=xxx&success=true&code=00

// 4. Frontend page /payment/result
useEffect(() => {
  const params = new URLSearchParams(window.location.search);
  const orderId = params.get('orderId');
  const success = params.get('success') === 'true';
  
  if (success) {
    // Show success message
    // Optionally poll for latest status
    pollPaymentStatus(orderId);
  } else {
    // Show error
  }
}, []);
```

### Mobile App (React Native / Flutter)
```javascript
// 1. Initiate payment
const response = await fetch('/api/payments/initiate', {
  method: 'POST',
  body: JSON.stringify({
    orderId: 'xxx',
    method: 'VNPAY',
    returnUrl: null  // Use default
  })
});

const { paymentUrl } = await response.json();

// 2. Open WebView or Browser
Linking.openURL(paymentUrl);

// 3. Setup deep link handler
Linking.addEventListener('url', (event) => {
  // myapp://payment/result?orderId=xxx&success=true&code=00
  const { orderId, success } = parseDeepLink(event.url);
  
  if (success) {
    navigation.navigate('PaymentSuccess', { orderId });
  } else {
    navigation.navigate('PaymentFailed', { orderId });
  }
});
```

---

## ⚙️ Config Hiện Tại

```yaml
vnpay:
  return-url: http://localhost:8080/api/payments/vnpay/return
  # ipn-url: (commented for localhost testing)
```

**Localhost Testing:**
- Return URL: ✅ Hoạt động (user redirect qua browser)
- IPN URL: ❌ Không hoạt động (VNPay sandbox không reach được localhost)

**Production:**
```yaml
vnpay:
  return-url: https://catholic-souvenir-api.southeastasia.cloudapp.azure.com/api/payments/vnpay/return
  ipn-url: https://catholic-souvenir-api.southeastasia.cloudapp.azure.com/api/payments/vnpay/ipn
```

---

## 🚨 Lưu Ý Quan Trọng

1. **LUÔN update DB qua IPN**, không phải Return URL
2. **Return URL chỉ để redirect user**, không tin tưởng data từ đây
3. **IPN phải return đúng format** `{"RspCode":"00","Message":"Confirm Success"}`
4. **Pass copy of params** khi verify signature: `new HashMap<>(params)`
5. **Frontend nên poll** `/api/payments/order/{orderId}/latest` để đảm bảo có status mới nhất

---

## 🎬 Demo Flow

1. User click "Thanh toán" → Call `/api/payments/initiate`
2. Backend return `paymentUrl` → User redirect đến VNPay
3. User nhập OTP `123456` → Click "Thanh toán"
4. VNPay gọi **2 callback đồng thời**:
   - Return URL → Redirect user về FE/Mobile
   - IPN URL → Update DB
5. User thấy success page → App poll để lấy status mới nhất
