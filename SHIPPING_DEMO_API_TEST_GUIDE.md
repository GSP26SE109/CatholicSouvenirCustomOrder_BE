# SHIPPING DEMO API TEST GUIDE

## ✅ Vấn đề đã được fix

Trước đây API `/shipments/demo/statuses` trả về GHN status (`ready_to_pick`, `picking`, etc.) nhưng hệ thống sử dụng enum `ShippingStatus` (`PENDING`, `PICKING`, etc.).

**Đã fix**:
1. ✅ API `/shipments/demo/statuses` bây giờ trả về đúng enum values
2. ✅ API `/shipments/demo/update-status` nhận enum values và tự động convert sang GHN status
3. ✅ Thêm validation cho status input
4. ✅ Cải thiện error handling

---

## API Endpoints

### 1. Lấy danh sách status có thể sử dụng

```bash
GET /api/shipments/demo/statuses
```

**Response**:
```json
{
  "code": 200,
  "message": "Lấy danh sách trạng thái thành công",
  "data": [
    {
      "value": "PENDING",
      "label": "Chờ lấy hàng",
      "description": "Đơn hàng đã được tạo, chờ shipper đến lấy"
    },
    {
      "value": "PICKING",
      "label": "Đang lấy hàng",
      "description": "Shipper đang trên đường đến lấy hàng"
    },
    {
      "value": "PICKED",
      "label": "Đã lấy hàng",
      "description": "Đã lấy hàng từ người gửi"
    },
    {
      "value": "STORING",
      "label": "Nhập kho",
      "description": "Hàng đang được lưu tại kho trung chuyển"
    },
    {
      "value": "TRANSPORTING",
      "label": "Đang vận chuyển",
      "description": "Hàng đang được vận chuyển đến kho đích"
    },
    {
      "value": "DELIVERING",
      "label": "Đang giao hàng",
      "description": "Shipper đang giao hàng đến người nhận"
    },
    {
      "value": "DELIVERED",
      "label": "Đã giao hàng",
      "description": "Giao hàng thành công"
    },
    {
      "value": "RETURNED",
      "label": "Hoàn trả",
      "description": "Hàng bị trả lại"
    },
    {
      "value": "CANCELLED",
      "label": "Đã hủy",
      "description": "Đơn hàng đã bị hủy"
    }
  ]
}
```

### 2. Cập nhật trạng thái shipping (Demo)

```bash
POST /api/shipments/demo/update-status
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "orderCode": "GHN123456789",
  "status": "PICKED"
}
```

**Valid status values** (sử dụng enum values):
- `PENDING` - Chờ lấy hàng
- `PICKING` - Đang lấy hàng
- `PICKED` - Đã lấy hàng
- `STORING` - Nhập kho
- `TRANSPORTING` - Đang vận chuyển
- `DELIVERING` - Đang giao hàng
- `DELIVERED` - Đã giao hàng
- `RETURNED` - Hoàn trả
- `CANCELLED` - Đã hủy

**Success Response**:
```json
{
  "code": 200,
  "message": "Demo webhook processed successfully. Order GHN123456789 updated to PICKED",
  "data": null
}
```

**Error Response (Invalid Status)**:
```json
{
  "code": 400,
  "message": "Invalid status. Valid values: PENDING, PICKING, PICKED, STORING, TRANSPORTING, DELIVERING, DELIVERED, RETURNED, CANCELLED",
  "data": null
}
```

---

## Test Scenarios

### Scenario 1: Cập nhật trạng thái từ PENDING → PICKED

1. **Tạo shipment** (hoặc sử dụng shipment có sẵn)
2. **Lấy orderCode** từ response hoặc database
3. **Cập nhật status**:
   ```bash
   curl -X POST http://localhost:8080/api/shipments/demo/update-status \
     -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "orderCode": "YOUR_ORDER_CODE",
       "status": "PICKED"
     }'
   ```

### Scenario 2: Test toàn bộ flow shipping

```bash
# 1. PENDING → PICKING
curl -X POST http://localhost:8080/api/shipments/demo/update-status \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"orderCode": "GHN123", "status": "PICKING"}'

# 2. PICKING → PICKED
curl -X POST http://localhost:8080/api/shipments/demo/update-status \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"orderCode": "GHN123", "status": "PICKED"}'

# 3. PICKED → STORING
curl -X POST http://localhost:8080/api/shipments/demo/update-status \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"orderCode": "GHN123", "status": "STORING"}'

# 4. STORING → TRANSPORTING
curl -X POST http://localhost:8080/api/shipments/demo/update-status \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"orderCode": "GHN123", "status": "TRANSPORTING"}'

# 5. TRANSPORTING → DELIVERING
curl -X POST http://localhost:8080/api/shipments/demo/update-status \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"orderCode": "GHN123", "status": "DELIVERING"}'

# 6. DELIVERING → DELIVERED
curl -X POST http://localhost:8080/api/shipments/demo/update-status \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"orderCode": "GHN123", "status": "DELIVERED"}'
```

### Scenario 3: Test error cases

```bash
# Invalid status
curl -X POST http://localhost:8080/api/shipments/demo/update-status \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"orderCode": "GHN123", "status": "INVALID_STATUS"}'

# Missing orderCode
curl -X POST http://localhost:8080/api/shipments/demo/update-status \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "PICKED"}'

# Missing status
curl -X POST http://localhost:8080/api/shipments/demo/update-status \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"orderCode": "GHN123"}'
```

---

## Mapping Logic

### Enum → GHN Status (trong controller)

| Enum Value | GHN Status | Description |
|------------|------------|-------------|
| PENDING | ready_to_pick | Chờ lấy hàng |
| PICKING | picking | Đang lấy hàng |
| PICKED | picked | Đã lấy hàng |
| STORING | storing | Nhập kho |
| TRANSPORTING | transporting | Đang vận chuyển |
| DELIVERING | delivering | Đang giao hàng |
| DELIVERED | delivered | Đã giao hàng |
| RETURNED | return | Hoàn trả |
| CANCELLED | cancel | Đã hủy |

### GHN Status → Enum (trong ShippingServiceImp)

Xem method `mapGHNStatusToShippingStatus` trong `ShippingServiceImp.java`

---

## Logs để debug

Khi test, check logs để xem quá trình mapping:

```
2026-04-19 10:30:15 INFO  ShippingController - DEMO: Simulating GHN webhook for order: GHN123, status: PICKED -> GHN: picked
2026-04-19 10:30:15 INFO  ShippingServiceImp - Processing GHN webhook for order: GHN123, status: picked
2026-04-19 10:30:15 INFO  ShippingServiceImp - Mapped GHN status 'picked' to ShippingStatus.PICKED
2026-04-19 10:30:15 INFO  ShippingServiceImp - Updated shipment status to PICKED for order: GHN123
```

---

## Troubleshooting

### Issue: Status không được cập nhật

**Possible causes**:
1. OrderCode không tồn tại trong database
2. Shipment chưa được tạo
3. Mapping logic có vấn đề

**Debug steps**:
1. Check logs xem có error không
2. Verify orderCode trong database
3. Check ShippingServiceImp.handleGHNWebhook method

### Issue: 403 Forbidden

**Cause**: Không có quyền ADMIN

**Solution**: Đảm bảo token có role ADMIN:
```bash
# Login as admin first
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "admin_password"
  }'
```

### Issue: 400 Bad Request - Invalid status

**Cause**: Sử dụng GHN status thay vì enum values

**Solution**: Sử dụng enum values (PENDING, PICKING, PICKED, etc.) thay vì GHN status (ready_to_pick, picking, picked, etc.)

---

**Tác giả**: Kiro AI Assistant  
**Ngày cập nhật**: 19/04/2026  
**Phiên bản**: 2.0 (Fixed enum mapping)
