# Kịch Bản Test API - Hệ Thống Hoàn Tiền

## Mục Lục
1. [Chuẩn Bị](#chuẩn-bị)
2. [Luồng 1: Hoàn Tiền Không Cần Trả Hàng](#luồng-1-hoàn-tiền-không-cần-trả-hàng)
3. [Luồng 2: Hoàn Tiền Có Trả Hàng](#luồng-2-hoàn-tiền-có-trả-hàng)
4. [Luồng 3: Admin Từ Chối Khiếu Nại](#luồng-3-admin-từ-chối-khiếu-nại)
5. [Luồng 4: Retry Refund Thất Bại](#luồng-4-retry-refund-thất-bại)
6. [Kiểm Tra Phân Quyền](#kiểm-tra-phân-quyền)

---

## Chuẩn Bị

### 1. Tạo Test Accounts
```bash
# Tạo 3 tài khoản test
# - Customer: customer@test.com / password123
# - Artisan: artisan@test.com / password123  
# - Admin: admin@test.com / password123
```

### 2. Lấy JWT Tokens
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "customer@test.com",
  "password": "password123"
}
```

**Lưu lại tokens:**
- `CUSTOMER_TOKEN` = Bearer eyJhbGc...
- `ARTISAN_TOKEN` = Bearer eyJhbGc...
- `ADMIN_TOKEN` = Bearer eyJhbGc...

### 3. Tạo Order/CustomOrder Test
```bash
# Tạo một đơn hàng hoàn tất (COMPLETED/DELIVERED)
# Lưu lại ORDER_ID hoặc CUSTOM_ORDER_ID
# Đảm bảo artisan wallet có đủ tiền để hoàn
```

### 4. Kiểm Tra Wallet Balance
```http
GET http://localhost:8080/api/wallet
Authorization: {{ARTISAN_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Lấy thông tin ví thành công",
  "data": {
    "walletId": "uuid",
    "balance": 1000000.00
  }
}
```

---

## Luồng 1: Hoàn Tiền Không Cần Trả Hàng

### Bước 1: Customer Tạo Khiếu Nại
```http
POST http://localhost:8080/api/complaints
Authorization: {{CUSTOMER_TOKEN}}
Content-Type: application/json

{
  "orderId": "{{ORDER_ID}}",
  "customOrderId": null,
  "productId": null,
  "reason": "Sản phẩm bị lỗi khi nhận được, màu sắc không đúng như mô tả và có vết xước trên bề mặt",
  "evidenceImages": [
    "https://example.com/evidence1.jpg",
    "https://example.com/evidence2.jpg"
  ]
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Tạo đơn khiếu nại thành công. Artisan sẽ được thông báo để xem xét.",
  "data": {
    "complaintId": "uuid",
    "status": "PENDING_ARTISAN_RESPONSE",
    "reason": "Sản phẩm bị lỗi...",
    "createdAt": "2026-04-18T10:00:00"
  }
}
```

**Lưu lại:** `COMPLAINT_ID`

### Bước 2: Customer Xem Danh Sách Khiếu Nại
```http
GET http://localhost:8080/api/complaints?page=0&size=10
Authorization: {{CUSTOMER_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Lấy danh sách đơn khiếu nại thành công",
  "data": {
    "content": [
      {
        "complaintId": "uuid",
        "status": "PENDING_ARTISAN_RESPONSE",
        "reason": "Sản phẩm bị lỗi..."
      }
    ],
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### Bước 3: Customer Xem Chi Tiết Khiếu Nại
```http
GET http://localhost:8080/api/complaints/{{COMPLAINT_ID}}
Authorization: {{CUSTOMER_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Lấy chi tiết đơn khiếu nại thành công",
  "data": {
    "complaintId": "uuid",
    "status": "PENDING_ARTISAN_RESPONSE",
    "reason": "Sản phẩm bị lỗi...",
    "evidenceImages": ["url1", "url2"],
    "customerName": "Nguyễn Văn A",
    "artisanName": "Thợ Thủ Công B"
  }
}
```

### Bước 4: Artisan Xem Khiếu Nại
```http
GET http://localhost:8080/api/artisan/complaints?page=0&size=10
Authorization: {{ARTISAN_TOKEN}}
```

### Bước 5: Artisan Xem Chi Tiết và Phản Hồi (Không Yêu Cầu Trả Hàng)
```http
POST http://localhost:8080/api/artisan/complaints/{{COMPLAINT_ID}}/respond
Authorization: {{ARTISAN_TOKEN}}
Content-Type: application/json

{
  "response": "Tôi xin lỗi về sự cố này. Đây là lỗi trong quá trình sản xuất. Tôi đồng ý hoàn tiền cho khách hàng mà không cần trả hàng.",
  "requireReturn": false
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Phản hồi đơn khiếu nại thành công. Admin sẽ xem xét và đưa ra quyết định.",
  "data": {
    "complaintId": "uuid",
    "status": "PENDING_ADMIN_REVIEW",
    "artisanResponse": "Tôi xin lỗi...",
    "requireReturn": false
  }
}
```

### Bước 6: Admin Xem Tất Cả Khiếu Nại
```http
GET http://localhost:8080/api/admin/complaints?status=PENDING_ADMIN_REVIEW&page=0&size=10
Authorization: {{ADMIN_TOKEN}}
```

### Bước 7: Admin Xem Chi Tiết Khiếu Nại
```http
GET http://localhost:8080/api/admin/complaints/{{COMPLAINT_ID}}
Authorization: {{ADMIN_TOKEN}}
```

### Bước 8: Admin Phê Duyệt Khiếu Nại (Hoàn Tiền Ngay)
```http
POST http://localhost:8080/api/admin/complaints/{{COMPLAINT_ID}}/approve
Authorization: {{ADMIN_TOKEN}}
Content-Type: application/json

{
  "refundAmount": 500000.00,
  "adminNote": "Khiếu nại hợp lệ. Phê duyệt hoàn tiền 500,000 VND cho khách hàng."
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Phê duyệt đơn khiếu nại thành công. Hệ thống đã xử lý hoàn tiền.",
  "data": {
    "complaintId": "uuid",
    "status": "APPROVED",
    "refundAmount": 500000.00,
    "requireReturn": false
  }
}
```

### Bước 9: Kiểm Tra Wallet Balance Sau Hoàn Tiền

**Customer Wallet:**
```http
GET http://localhost:8080/api/wallet
Authorization: {{CUSTOMER_TOKEN}}
```

**Expected:** Balance tăng 500,000 VND

**Artisan Wallet:**
```http
GET http://localhost:8080/api/wallet
Authorization: {{ARTISAN_TOKEN}}
```

**Expected:** Balance giảm 500,000 VND

### Bước 10: Kiểm Tra Transaction History

**Customer:**
```http
GET http://localhost:8080/api/wallet/transactions
Authorization: {{CUSTOMER_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "data": [
    {
      "type": "REFUND_CREDIT",
      "amount": 500000.00,
      "description": "Refund credit for complaint #uuid",
      "balanceBefore": 0.00,
      "balanceAfter": 500000.00
    }
  ]
}
```

**Artisan:**
```http
GET http://localhost:8080/api/wallet/transactions
Authorization: {{ARTISAN_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "data": [
    {
      "type": "REFUND_DEBIT",
      "amount": -500000.00,
      "description": "Refund debit for complaint #uuid",
      "balanceBefore": 1000000.00,
      "balanceAfter": 500000.00
    }
  ]
}
```

### Bước 11: Admin Xem Refund Transactions
```http
GET http://localhost:8080/api/admin/complaints/refund-transactions?status=COMPLETED&page=0&size=10
Authorization: {{ADMIN_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Lấy danh sách giao dịch hoàn tiền thành công",
  "data": {
    "content": [
      {
        "refundTransactionId": "uuid",
        "complaintId": "uuid",
        "amount": 500000.00,
        "status": "COMPLETED",
        "fromWalletOwnerName": "Thợ Thủ Công B",
        "toWalletOwnerName": "Nguyễn Văn A",
        "completedAt": "2026-04-18T10:30:00"
      }
    ]
  }
}
```

---

## Luồng 2: Hoàn Tiền Có Trả Hàng

### Bước 1-5: Giống Luồng 1

### Bước 6: Artisan Phản Hồi (Yêu Cầu Trả Hàng)
```http
POST http://localhost:8080/api/artisan/complaints/{{COMPLAINT_ID}}/respond
Authorization: {{ARTISAN_TOKEN}}
Content-Type: application/json

{
  "response": "Tôi cần kiểm tra sản phẩm trước khi hoàn tiền. Vui lòng gửi sản phẩm về để tôi xác nhận tình trạng.",
  "requireReturn": true
}
```

### Bước 7: Admin Phê Duyệt (Yêu Cầu Trả Hàng)
```http
POST http://localhost:8080/api/admin/complaints/{{COMPLAINT_ID}}/approve
Authorization: {{ADMIN_TOKEN}}
Content-Type: application/json

{
  "refundAmount": 500000.00,
  "adminNote": "Phê duyệt khiếu nại. Khách hàng cần trả hàng trước khi hoàn tiền."
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Phê duyệt đơn khiếu nại thành công. Khách hàng cần trả hàng trước khi hoàn tiền.",
  "data": {
    "complaintId": "uuid",
    "status": "APPROVED",
    "refundAmount": 500000.00,
    "requireReturn": true
  }
}
```

### Bước 8: Customer Tạo Return Shipment
```http
POST http://localhost:8080/api/complaints/{{COMPLAINT_ID}}/return
Authorization: {{CUSTOMER_TOKEN}}
Content-Type: application/json

{
  "fromName": "Nguyễn Văn A",
  "fromPhone": "0901234567",
  "fromAddress": "123 Đường ABC",
  "fromWardCode": "20308",
  "fromDistrictId": 1442,
  "fromProvinceId": 202,
  "toName": "Thợ Thủ Công B",
  "toPhone": "0907654321",
  "toAddress": "456 Đường XYZ",
  "toWardCode": "20101",
  "toDistrictId": 1443,
  "toProvinceId": 202,
  "weight": 500,
  "length": 20,
  "width": 15,
  "height": 10,
  "note": "Trả hàng theo khiếu nại"
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Tạo đơn trả hàng thành công. Vui lòng gửi hàng về địa chỉ của Artisan.",
  "data": {
    "shipmentId": "uuid",
    "trackingNumber": "GHN123456",
    "status": "PENDING"
  }
}
```

**Lưu lại:** `RETURN_SHIPMENT_ID`

### Bước 9: Artisan Xác Nhận Nhận Hàng Trả Về
```http
POST http://localhost:8080/api/artisan/return-shipments/{{RETURN_SHIPMENT_ID}}/confirm
Authorization: {{ARTISAN_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Xác nhận nhận hàng trả về thành công. Hệ thống sẽ xử lý hoàn tiền.",
  "data": {
    "shipmentId": "uuid",
    "status": "DELIVERED"
  }
}
```

### Bước 10: Kiểm Tra Complaint Status
```http
GET http://localhost:8080/api/complaints/{{COMPLAINT_ID}}
Authorization: {{CUSTOMER_TOKEN}}
```

**Expected:** Status = `REFUNDED`

### Bước 11: Kiểm Tra Wallet Balance (Giống Luồng 1 Bước 9-11)

---

## Luồng 3: Admin Từ Chối Khiếu Nại

### Bước 1-6: Giống Luồng 1

### Bước 7: Admin Từ Chối Khiếu Nại
```http
POST http://localhost:8080/api/admin/complaints/{{COMPLAINT_ID}}/reject
Authorization: {{ADMIN_TOKEN}}
Content-Type: application/json

{
  "rejectionReason": "Sau khi xem xét bằng chứng, khiếu nại không đủ cơ sở. Sản phẩm đã được giao đúng mô tả."
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Từ chối đơn khiếu nại thành công. Khách hàng đã được thông báo.",
  "data": {
    "complaintId": "uuid",
    "status": "REJECTED",
    "rejectionReason": "Sau khi xem xét..."
  }
}
```

### Bước 8: Customer Xem Complaint Bị Từ Chối
```http
GET http://localhost:8080/api/complaints/{{COMPLAINT_ID}}
Authorization: {{CUSTOMER_TOKEN}}
```

**Expected:** Status = `REJECTED`, có rejectionReason

---

## Luồng 4: Retry Refund Thất Bại

### Điều Kiện: Artisan wallet không đủ tiền

### Bước 1: Tạo Complaint và Approve (Artisan wallet balance < refund amount)

### Bước 2: Kiểm Tra Refund Transaction Failed
```http
GET http://localhost:8080/api/admin/complaints/refund-transactions?status=FAILED&page=0&size=10
Authorization: {{ADMIN_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "refundTransactionId": "uuid",
        "status": "FAILED",
        "failureReason": "Insufficient balance in artisan wallet. Required: 500000, Available: 100000"
      }
    ]
  }
}
```

**Lưu lại:** `FAILED_REFUND_TRANSACTION_ID`

### Bước 3: Admin Nạp Tiền Vào Artisan Wallet (Manual)
```bash
# Giả lập: Thêm tiền vào artisan wallet qua database hoặc payment
```

### Bước 4: Admin Retry Refund
```http
POST http://localhost:8080/api/admin/complaints/refund-transactions/{{FAILED_REFUND_TRANSACTION_ID}}/retry
Authorization: {{ADMIN_TOKEN}}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Thử lại hoàn tiền thành công",
  "data": {
    "refundTransactionId": "new-uuid",
    "status": "COMPLETED",
    "amount": 500000.00
  }
}
```

### Bước 5: Kiểm Tra Wallet Balance
```http
GET http://localhost:8080/api/wallet
Authorization: {{CUSTOMER_TOKEN}}
```

**Expected:** Balance đã được cộng tiền

---

## Kiểm Tra Phân Quyền

### Test 1: Customer Không Thể Xem Complaint Của Người Khác
```http
GET http://localhost:8080/api/complaints/{{OTHER_CUSTOMER_COMPLAINT_ID}}
Authorization: {{CUSTOMER_TOKEN}}
```

**Expected:** 403 Forbidden

### Test 2: Artisan Không Thể Xem Complaint Không Phải Của Mình
```http
GET http://localhost:8080/api/artisan/complaints/{{OTHER_ARTISAN_COMPLAINT_ID}}
Authorization: {{ARTISAN_TOKEN}}
```

**Expected:** 403 Forbidden

### Test 3: Customer Không Thể Approve Complaint
```http
POST http://localhost:8080/api/admin/complaints/{{COMPLAINT_ID}}/approve
Authorization: {{CUSTOMER_TOKEN}}
Content-Type: application/json

{
  "refundAmount": 500000.00,
  "adminNote": "Test"
}
```

**Expected:** 403 Forbidden

### Test 4: Artisan Không Thể Retry Refund
```http
POST http://localhost:8080/api/admin/complaints/refund-transactions/{{REFUND_TRANSACTION_ID}}/retry
Authorization: {{ARTISAN_TOKEN}}
```

**Expected:** 403 Forbidden

---

## Validation Tests

### Test 1: Reason Quá Ngắn
```http
POST http://localhost:8080/api/complaints
Authorization: {{CUSTOMER_TOKEN}}
Content-Type: application/json

{
  "orderId": "{{ORDER_ID}}",
  "reason": "Lỗi"
}
```

**Expected:** 400 Bad Request - "Lý do khiếu nại phải từ 20 đến 1000 ký tự"

### Test 2: Quá Nhiều Evidence Images
```http
POST http://localhost:8080/api/complaints
Authorization: {{CUSTOMER_TOKEN}}
Content-Type: application/json

{
  "orderId": "{{ORDER_ID}}",
  "reason": "Sản phẩm bị lỗi khi nhận được hàng",
  "evidenceImages": ["url1", "url2", "url3", "url4", "url5", "url6"]
}
```

**Expected:** 400 Bad Request - "Tối đa 5 hình ảnh bằng chứng"

### Test 3: Refund Amount = 0
```http
POST http://localhost:8080/api/admin/complaints/{{COMPLAINT_ID}}/approve
Authorization: {{ADMIN_TOKEN}}
Content-Type: application/json

{
  "refundAmount": 0,
  "adminNote": "Test validation"
}
```

**Expected:** 400 Bad Request - "Số tiền hoàn phải lớn hơn 0"

---

## Checklist Tổng Hợp

### Luồng Hoàn Tiền Không Trả Hàng
- [ ] Customer tạo complaint thành công
- [ ] Customer xem danh sách và chi tiết complaint
- [ ] Artisan xem và phản hồi complaint (requireReturn = false)
- [ ] Admin xem và approve complaint
- [ ] Refund transaction được tạo với status COMPLETED
- [ ] Customer wallet tăng đúng số tiền
- [ ] Artisan wallet giảm đúng số tiền
- [ ] Wallet transactions được ghi nhận đúng
- [ ] Complaint status = REFUNDED

### Luồng Hoàn Tiền Có Trả Hàng
- [ ] Artisan phản hồi với requireReturn = true
- [ ] Admin approve với requireReturn = true
- [ ] Customer tạo return shipment thành công
- [ ] Artisan confirm nhận hàng
- [ ] Refund được xử lý sau khi confirm
- [ ] Complaint status = REFUNDED

### Luồng Từ Chối
- [ ] Admin reject complaint thành công
- [ ] Complaint status = REJECTED
- [ ] Không có refund transaction được tạo
- [ ] Customer nhận được thông báo

### Luồng Retry
- [ ] Refund failed khi artisan không đủ tiền
- [ ] Failed transaction được ghi nhận
- [ ] Admin có thể retry sau khi artisan có tiền
- [ ] Retry thành công tạo transaction mới

### Phân Quyền
- [ ] Customer chỉ xem được complaint của mình
- [ ] Artisan chỉ xem được complaint liên quan
- [ ] Chỉ Admin có thể approve/reject
- [ ] Chỉ Admin có thể retry refund

### Validation
- [ ] Reason phải từ 20-1000 ký tự
- [ ] Evidence images tối đa 5
- [ ] Refund amount phải > 0
- [ ] Admin note phải từ 10-500 ký tự
- [ ] Rejection reason phải từ 20-500 ký tự

---

## Notes

1. **Base URL:** Thay `http://localhost:8080` bằng URL server của bạn
2. **UUIDs:** Thay các `{{VARIABLE}}` bằng UUID thực tế từ response
3. **Tokens:** Nhớ refresh token nếu hết hạn
4. **Database:** Có thể cần reset database giữa các test để tránh conflict
5. **GHN API:** Return shipment cần GHN API key hợp lệ
6. **Notifications:** Kiểm tra notifications sau mỗi action quan trọng

## Tools Đề Xuất

- **Postman:** Import collection để test nhanh
- **Bruno:** Alternative cho Postman
- **curl:** Script automation
- **JMeter:** Load testing

## Troubleshooting

### Lỗi 401 Unauthorized
- Kiểm tra token còn hạn
- Đảm bảo format: `Authorization: Bearer <token>`

### Lỗi 403 Forbidden
- Kiểm tra role của user
- Đảm bảo user có quyền truy cập resource

### Lỗi 404 Not Found
- Kiểm tra UUID có tồn tại
- Kiểm tra endpoint URL đúng

### Refund Failed
- Kiểm tra artisan wallet balance
- Xem logs để biết lý do cụ thể
- Kiểm tra complaint status có phải APPROVED không
