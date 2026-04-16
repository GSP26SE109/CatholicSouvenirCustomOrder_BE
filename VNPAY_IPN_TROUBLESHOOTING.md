# VNPay IPN Troubleshooting Guide

## Vấn đề hiện tại
VNPay không callback về IPN endpoint để update DB, payment vẫn ở trạng thái PENDING.

## Nguyên nhân có thể

### 1. VNPay Sandbox Delay
VNPay sandbox có thể có delay 1-5 phút trước khi gọi IPN. Đợi thêm vài phút và check logs.

### 2. IPN URL không accessible
VNPay không thể reach được server của bạn:
- Check firewall/security group
- Check server có public IP không
- Check port 80/443 có mở không

### 3. IPN Response Format Sai
VNPay expect response format cụ thể:
```json
{
  "RspCode": "00",
  "Message": "Confirm Success"
}
```

## Giải pháp hiện tại

Code đã được update để handle cả 2 trường hợp:

### 1. Return URL Callback (Đang hoạt động)
```java
@GetMapping("/vnpay/return")
public void handleVNPayReturn(...) {
    // Update DB here (backup for IPN)
    paymentService.handlePaymentCallback(request);
    
    // Redirect to frontend
    response.sendRedirect(redirectUrl);
}
```

### 2. IPN Callback (Backup)
```java
@GetMapping("/vnpay/ipn")
public ResponseEntity<Map<String, String>> handleVNPayIPN(...) {
    // Update DB
    paymentService.handlePaymentCallback(request);
    
    // Return proper format
    return ResponseEntity.ok(Map.of(
        "RspCode", "00",
        "Message", "Confirm Success"
    ));
}
```

## Cách test IPN endpoint

### Test 1: Manual curl request
```bash
curl -X GET "https://catholic-souvenir-api.southeastasia.cloudapp.azure.com/api/payments/vnpay/ipn?vnp_Amount=30000000&vnp_BankCode=NCB&vnp_ResponseCode=00&vnp_TxnRef=GROUP_xxx&vnp_SecureHash=xxx"
```

Expected response:
```json
{
  "RspCode": "00",
  "Message": "Confirm Success"
}
```

### Test 2: Check server logs
Tìm log entries:
```
Received VNPay IPN notification
All IPN params: {...}
Filtered VNPay params: {...}
VNPay signature verified successfully
Payment successful, updating status
```

### Test 3: Check database
```sql
SELECT * FROM payments 
WHERE reference_id = 'GROUP_xxx' 
ORDER BY created_at DESC;
```

Status should be 'SUCCESS' not 'PENDING'.

## Workaround hiện tại

Vì `/vnpay/return` endpoint đã update DB, nên payment sẽ được update ngay khi user redirect về. IPN chỉ là backup.

**Flow hiện tại:**
1. User thanh toán → VNPay redirect về `/vnpay/return`
2. Backend update DB tại `/vnpay/return`
3. Backend redirect user về frontend
4. VNPay gọi IPN (có thể delay) → Idempotent update (không ảnh hưởng nếu đã update)

## Kiểm tra payment đã được update chưa

1. Check logs xem có dòng này không:
```
Payment status updated successfully via return URL
```

2. Check DB:
```sql
SELECT status, paid_at FROM payments WHERE reference_id = 'GROUP_7178426d-2372-4b2d-b1d5-0baf80ee215e_xxx';
```

3. Check orders:
```sql
SELECT status FROM orders WHERE order_group_id = '7178426d-2372-4b2d-b1d5-0baf80ee215e';
```

## Nếu vẫn PENDING

Có thể là:
1. Return URL callback bị lỗi → Check logs
2. Signature verification failed → Check hash secret
3. Exception trong handlePaymentCallback → Check logs

## Debug steps

1. Restart server để clear cache
2. Tạo payment mới
3. Thanh toán test
4. Check logs ngay lập tức:
```bash
tail -f /var/log/app.log | grep -i "vnpay\|payment"
```

5. Nếu thấy "Payment status updated successfully" → OK
6. Nếu không thấy → Check exception trong logs

## Production recommendations

1. Monitor IPN callback rate
2. Set up alerting nếu IPN fail rate > 10%
3. Có retry mechanism cho failed payments
4. Log tất cả IPN requests để debug
