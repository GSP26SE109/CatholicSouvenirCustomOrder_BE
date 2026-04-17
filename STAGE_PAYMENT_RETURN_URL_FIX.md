# Stage Payment Return URL Implementation

## Vấn đề
Stage payment (Request-Based Custom Order) chưa có returnUrl flow như checkout payment, dẫn đến:
- Không redirect về frontend sau khi thanh toán
- Frontend không nhận được payment result
- User experience không tốt

## Giải pháp đã implement

### 1. Thêm returnUrl vào StagePayment model
```java
@Column(length = 500)
private String returnUrl;
```

### 2. Custom query trong StagePaymentRepository
```java
@Query("SELECT DISTINCT sp FROM StagePayment sp " +
       "LEFT JOIN FETCH sp.stage s " +
       "LEFT JOIN FETCH s.customOrder co " +
       "LEFT JOIN FETCH co.request cr " +
       "LEFT JOIN FETCH cr.selectedArtisan a " +
       "WHERE sp.paymentId = :paymentId")
Optional<StagePayment> findByIdWithDetailsForDistribution(@Param("paymentId") UUID paymentId);
```
- Fetch tất cả data cần thiết với JOIN FETCH
- Tránh lazy loading issues khi distribute payment

### 3. Update StagePaymentServiceImp
**Thêm overloaded method:**
```java
public StagePaymentResponse createStagePayment(UUID stageId, UUID customerId, 
                                               String paymentMethod, String returnUrl)
```

**Key changes:**
- Lưu returnUrl vào database
- Sử dụng backend callback URL: `/api/stage-payments/vnpay/return`
- Re-fetch payment với JOIN FETCH trước khi distribute
- Wrap distribution trong try-catch

**Thêm method mới:**
```java
public String getReturnUrlByReferenceId(String referenceId)
```

### 4. Update WalletServiceImp.processStagePaymentDistribution()
**Improvements:**
- Safe navigation khi get stageId (try-catch)
- Validate artisan không null
- Không fail nếu không get được stageId

### 5. Thêm return endpoint trong StagePaymentController
**New endpoint:**
```java
GET /api/stage-payments/vnpay/return
```

**Flow:**
1. Nhận callback từ VNPay
2. Process payment (update status, distribute money)
3. Query returnUrl từ database
4. Redirect về frontend với payment result parameters

**Helper methods:**
- `getReturnUrlFromPayment()` - Get returnUrl from service
- `buildRedirectUrl()` - Build URL với payment result
- `buildDefaultRedirectUrl()` - Fallback URL

### 6. Update CustomOrderStageServiceImp
**Method `initiateStagePayment()`:**
- Pass returnUrl từ request vào StagePaymentService
- Cast service to StagePaymentServiceImp để gọi overloaded method

## Flow hoàn chỉnh

```
1. Customer → POST /api/stage-payments/{stageId}/initiate
   Body: { paymentMethod: "VNPAY", returnUrl: "http://frontend/result" }

2. Backend → Create StagePayment record
   - Save returnUrl to database
   - Generate VNPay URL with backend callback

3. Customer → Redirect to VNPay payment page

4. Customer → Complete payment on VNPay

5. VNPay → GET /api/stage-payments/vnpay/return?vnp_TxnRef=...&vnp_ResponseCode=00
   
6. Backend → Process payment
   - Verify signature
   - Update payment status
   - Distribute money (90% artisan, 10% platform)
   - Update stage status to PAID
   
7. Backend → Query returnUrl from database

8. Backend → Redirect to frontend
   Location: {returnUrl}?paymentId=xxx&status=SUCCESS&stageId=xxx
```

## URL Parameters khi redirect về frontend

```
?paymentId={UUID}
&status={SUCCESS|FAILED}
&responseCode={00|...}
&transactionId={gateway_transaction_id}
&stageId={UUID}
```

## Default URLs

**Backend callback:** `http://localhost:8080/api/stage-payments/vnpay/return`

**Frontend default:** `http://localhost:3000/stage-payment/result`

## Testing

1. Create custom order với stages
2. Initiate payment cho stage đầu tiên:
```json
POST /api/stage-payments/{stageId}/initiate
{
  "paymentMethod": "VNPAY",
  "returnUrl": "http://localhost:3000/custom-order/payment-result"
}
```
3. Complete payment trên VNPay sandbox
4. Verify redirect về returnUrl với correct parameters
5. Check wallet balances:
   - Artisan wallet: +90% of stage amount
   - Platform admin wallet: +10% of stage amount
6. Verify stage status = PAID

## Kết quả
✅ Stage payment có returnUrl flow giống checkout payment
✅ Automatic distribution sau khi payment thành công
✅ Safe navigation tránh lazy loading issues
✅ Proper error handling với try-catch
✅ Code compile thành công
