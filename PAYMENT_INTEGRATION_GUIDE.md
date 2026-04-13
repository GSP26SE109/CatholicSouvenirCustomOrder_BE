# Payment Integration Guide

## Overview
Hướng dẫn tích hợp thanh toán cho Web và Mobile App với VNPay và ZaloPay.

## API Endpoint
```
POST /api/payments/initiate
```

## Request Body

### Web Application
```json
{
  "orderId": "uuid-of-order",
  "method": "VNPAY",
  "returnUrl": "https://yourwebsite.com/payment/success",
  "cancelUrl": "https://yourwebsite.com/payment/cancel"
}
```

### Mobile Application
```json
{
  "orderId": "uuid-of-order",
  "method": "VNPAY",
  "returnUrl": "yourapp://payment/success",
  "cancelUrl": "yourapp://payment/cancel"
}
```

## Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| orderId | UUID | Yes | ID của order cần thanh toán |
| method | String | Yes | `VNPAY` hoặc `ZALOPAY` |
| returnUrl | String | No | URL redirect sau khi thanh toán (success/fail). Nếu null, dùng default từ config |
| cancelUrl | String | No | URL redirect khi user hủy thanh toán. Hiện tại chưa được VNPay/ZaloPay sử dụng |

## Response
```json
{
  "code": 200,
  "message": "Khởi tạo thanh toán thành công",
  "data": {
    "paymentId": "uuid-of-payment",
    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
    "transactionId": "ORDER_uuid_timestamp",
    "amount": 500000
  }
}
```

## Integration Flow

### Web Flow
1. User click "Thanh toán"
2. Frontend gọi API `/api/payments/initiate` với `returnUrl` là URL của web
3. Backend trả về `paymentUrl`
4. Frontend redirect user đến `paymentUrl` (VNPay/ZaloPay)
5. User thanh toán trên gateway
6. Gateway redirect về `returnUrl` với query params chứa kết quả
7. Frontend parse query params và hiển thị kết quả

### Mobile Flow
1. User click "Thanh toán"
2. App gọi API `/api/payments/initiate` với `returnUrl` là deep link của app
3. Backend trả về `paymentUrl`
4. App mở WebView hoặc browser với `paymentUrl`
5. User thanh toán trên gateway
6. Gateway redirect về deep link `yourapp://payment/success`
7. App handle deep link và hiển thị kết quả

## Deep Link Setup (Mobile)

### Android (AndroidManifest.xml)
```xml
<activity android:name=".PaymentResultActivity">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="yourapp"
            android:host="payment" />
    </intent-filter>
</activity>
```

### iOS (Info.plist)
```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>yourapp</string>
        </array>
    </dict>
</array>
```

## Payment Callback Parameters

### VNPay Success
```
yourapp://payment/success?vnp_Amount=50000000&vnp_BankCode=NCB&vnp_ResponseCode=00&vnp_TxnRef=ORDER_xxx&vnp_TransactionNo=123456
```

### VNPay Failed
```
yourapp://payment/success?vnp_ResponseCode=24&vnp_TxnRef=ORDER_xxx
```

### Response Codes
- `00`: Success
- `24`: User cancelled
- Other: Failed (see VNPay docs)

## Example Code

### React (Web)
```javascript
const initiatePayment = async (orderId) => {
  const response = await fetch('/api/payments/initiate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      orderId: orderId,
      method: 'VNPAY',
      returnUrl: `${window.location.origin}/payment/result`,
      cancelUrl: `${window.location.origin}/payment/cancel`
    })
  });
  
  const data = await response.json();
  
  // Redirect to payment gateway
  window.location.href = data.data.paymentUrl;
};

// Handle return from payment gateway
const PaymentResultPage = () => {
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const responseCode = params.get('vnp_ResponseCode');
    
    if (responseCode === '00') {
      // Payment success
      showSuccess('Thanh toán thành công!');
    } else {
      // Payment failed
      showError('Thanh toán thất bại!');
    }
  }, []);
  
  return <div>Processing...</div>;
};
```

### React Native (Mobile)
```javascript
import { Linking } from 'react-native';
import InAppBrowser from 'react-native-inappbrowser-reborn';

const initiatePayment = async (orderId) => {
  const response = await fetch('/api/payments/initiate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      orderId: orderId,
      method: 'VNPAY',
      returnUrl: 'yourapp://payment/success',
      cancelUrl: 'yourapp://payment/cancel'
    })
  });
  
  const data = await response.json();
  
  // Open payment URL in browser
  if (await InAppBrowser.isAvailable()) {
    await InAppBrowser.open(data.data.paymentUrl, {
      dismissButtonStyle: 'cancel',
      preferredBarTintColor: '#453AA4',
      preferredControlTintColor: 'white'
    });
  }
};

// Handle deep link
useEffect(() => {
  const handleDeepLink = (event) => {
    const url = event.url;
    
    if (url.startsWith('yourapp://payment/success')) {
      const params = new URLSearchParams(url.split('?')[1]);
      const responseCode = params.get('vnp_ResponseCode');
      
      if (responseCode === '00') {
        navigation.navigate('PaymentSuccess');
      } else {
        navigation.navigate('PaymentFailed');
      }
    }
  };
  
  Linking.addEventListener('url', handleDeepLink);
  
  return () => {
    Linking.removeEventListener('url', handleDeepLink);
  };
}, []);
```

## Testing

### Test URLs
- Web: `http://localhost:3000/payment/success`
- Mobile: `yourapp://payment/success`

### VNPay Sandbox Test Cards
- Card Number: `9704198526191432198`
- Name: `NGUYEN VAN A`
- Issue Date: `07/15`
- OTP: `123456`

## Notes
- `returnUrl` là bắt buộc cho production
- Nếu không truyền `returnUrl`, hệ thống sẽ dùng URL mặc định từ config
- `cancelUrl` hiện tại chưa được VNPay/ZaloPay sử dụng (họ dùng `returnUrl` cho cả success và fail)
- Mobile app cần config deep link scheme trước khi test
- Web cần handle query parameters từ payment gateway
