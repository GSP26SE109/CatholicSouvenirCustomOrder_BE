# Hướng Dẫn Tích Hợp Giao Hàng Nhanh (GHN)

## 📦 Tổng Quan

Hệ thống đã tích hợp GHN Sandbox để quản lý vận chuyển cho cả:
- Regular Orders (sản phẩm có sẵn)
- Custom Orders (sản phẩm tùy chỉnh)

---

## 🔑 Cấu Hình

### 1. Đăng Ký GHN Sandbox

1. Truy cập: https://sso.ghn.vn/
2. Đăng ký tài khoản
3. Tạo shop trong dashboard
4. Lấy API Token và Shop ID

### 2. Cấu Hình application.yml

```yaml
ghn:
  api-url: https://dev-online-gateway.ghn.vn
  token: your-ghn-token
  shop-id: your-shop-id
  from-district-id: 1442  # Quận 1, TP.HCM
  from-ward-code: 21211   # Phường Bến Nghé
```

### 3. Lấy District ID và Ward Code

```http
GET https://dev-online-gateway.ghn.vn/shiip/public-api/master-data/province
GET https://dev-online-gateway.ghn.vn/shiip/public-api/master-data/district
GET https://dev-online-gateway.ghn.vn/shiip/public-api/master-data/ward
Headers: Token: your-token
```

---

## 🚀 API Endpoints

### 1. Tạo Đơn Vận Chuyển

```http
POST /api/shipments
Authorization: Bearer {artisan-token}

{
  "orderId": "uuid",
  "recipientName": "Nguyễn Văn A",
  "recipientPhone": "0901234567",
  "deliveryAddress": "123 Lê Lợi",
  "toDistrictId": 1442,
  "toWardCode": "21211",
  "orderValue": 500000,
  "weight": 1000,
  "length": 20,
  "width": 20,
  "height": 10,
  "note": "Giao giờ hành chính",
  "serviceTypeId": 2,
  "paymentTypeId": 1
}
```

**Service Types:**
- 1: Express (Hỏa tốc)
- 2: Standard (Tiêu chuẩn)

**Payment Types:**
- 1: Shop trả phí
- 2: Người nhận trả phí (COD)

### 2. Tra Cứu Vận Đơn

```http
GET /api/shipments/track/{trackingNumber}
```

### 3. Tính Phí Vận Chuyển

```http
POST /api/shipments/calculate-fee

{
  "toDistrictId": 1442,
  "toWardCode": "21211",
  "orderValue": 500000,
  "weight": 1000,
  "length": 20,
  "width": 20,
  "height": 10,
  "serviceTypeId": 2
}
```

---

## 📊 Shipping Status Flow

```
PENDING (ready_to_pick)
  ↓
PICKING (picking)
  ↓
PICKED (picked)
  ↓
STORING (storing)
  ↓
TRANSPORTING (transporting)
  ↓
DELIVERING (delivering)
  ↓
DELIVERED (delivered)
```

---

## 🎯 GHN API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/shiip/public-api/v2/shipping-order/create` | POST | Tạo đơn hàng |
| `/shiip/public-api/v2/shipping-order/detail` | POST | Chi tiết đơn hàng |
| `/shiip/public-api/v2/shipping-order/fee` | POST | Tính phí vận chuyển |
| `/shiip/public-api/v2/switch-status/cancel` | POST | Hủy đơn hàng |

---

## 🧪 Testing với Sandbox

### Test Data (TP.HCM):

```
From (Quận 1):
- District ID: 1442
- Ward Code: 21211

To (Quận 3):
- District ID: 1444
- Ward Code: 21309
```

---

Generated: 2026-03-28


## 📦 Tổng Quan

Hệ thống đã tích hợp GHTK Sandbox để quản lý vận chuyển cho cả:
- Regular Orders (sản phẩm có sẵn)
- Custom Orders (sản phẩm tùy chỉnh)

---

## 🔑 Cấu Hình

### 1. Đăng Ký GHTK Sandbox

1. Truy cập: https://khachhang.ghtklab.com/
2. Đăng ký tài khoản sandbox
3. Lấy API Token từ dashboard

### 2. Cấu Hình application.yml

```yaml
ghtk:
  api-url: https://services.ghtklab.com
  token: your-ghtk-sandbox-token
  pick-name: Catholic Souvenir Shop
  pick-address: 123 Nguyen Hue, District 1
  pick-province: TP. Hồ Chí Minh
  pick-district: Quận 1
  pick-ward: Phường Bến Nghé
  pick-tel: 0901234567
```

### 3. Environment Variables (Production)

```bash
export GHTK_TOKEN=your-production-token
export GHTK_PICK_NAME="Your Shop Name"
export GHTK_PICK_ADDRESS="Your Address"
```

---

## 🚀 API Endpoints

### 1. Tạo Đơn Vận Chuyển

```http
POST /api/shipments
Authorization: Bearer {artisan-token}

{
  "orderId": "uuid",  // hoặc customOrderId
  "recipientName": "Nguyễn Văn A",
  "recipientPhone": "0901234567",
  "deliveryAddress": "123 Lê Lợi",
  "province": "TP. Hồ Chí Minh",
  "district": "Quận 1",
  "ward": "Phường Bến Nghé",
  "orderValue": 500000,
  "note": "Giao giờ hành chính",
  "isFreeship": false
}
```

### 2. Tra Cứu Vận Đơn

```http
GET /api/shipments/track/{trackingNumber}
```

### 3. Tính Phí Vận Chuyển

```http
POST /api/shipments/calculate-fee

{
  "province": "TP. Hồ Chí Minh",
  "district": "Quận 1",
  "orderValue": 500000
}
```

### 4. Hủy Đơn Vận Chuyển

```http
POST /api/shipments/{shipmentId}/cancel
Authorization: Bearer {artisan-token}
```

---

## 📊 Shipping Status Flow

```
PENDING (Chờ lấy hàng)
  ↓
PICKING (Đang lấy hàng)
  ↓
PICKED (Đã lấy hàng)
  ↓
STORING (Nhập kho)
  ↓
TRANSPORTING (Đang vận chuyển)
  ↓
DELIVERING (Đang giao hàng)
  ↓
DELIVERED (Đã giao hàng)
```

**Trạng thái đặc biệt:**
- RETURNED: Hoàn trả
- CANCELLED: Đã hủy

---

## 🔄 Workflow Tích Hợp

### Regular Order Flow:

```
1. Customer đặt hàng → Order created
2. Customer thanh toán → Payment success
3. Artisan xác nhận → Order IN_PRODUCTION
4. Artisan tạo shipment → POST /api/shipments
5. GHTK lấy hàng → Status PICKING
6. Vận chuyển → Status TRANSPORTING
7. Giao hàng → Status DELIVERED
8. Update Order status → COMPLETED
```

### Custom Order Flow:

```
1. Customer tạo CustomRequest
2. Artisan accept → CustomOrder created
3. Customer thanh toán
4. Artisan sản xuất → CustomOrder IN_PRODUCTION
5. Artisan tạo shipment → POST /api/shipments
6. GHTK vận chuyển
7. Giao hàng thành công → CustomOrder COMPLETED
```

---

## 🎯 GHTK API Mapping

### Status ID Mapping:

| GHTK Status ID | Our Status | Description |
|----------------|------------|-------------|
| 1 | PENDING | Chờ lấy hàng |
| 2 | PICKING | Đang lấy hàng |
| 3 | PICKED | Đã lấy hàng |
| 4 | STORING | Nhập kho |
| 5 | TRANSPORTING | Đang vận chuyển |
| 6 | DELIVERING | Đang giao hàng |
| 7 | DELIVERED | Đã giao hàng |
| 9 | RETURNED | Hoàn trả |
| 13 | CANCELLED | Đã hủy |

---

## 🔔 Webhook Integration

### Setup Webhook URL:

```
https://your-domain.com/api/shipments/webhook
```

### Webhook Payload Example:

```json
{
  "order_label": "S12345678",
  "status_id": 7,
  "status_text": "Đã giao hàng",
  "deliver_date": "2024-03-28T10:30:00"
}
```

### Handle Webhook:

```java
@PostMapping("/webhook")
public ResponseEntity<BaseResponse> ghtkWebhook(@RequestBody Map<String, Object> payload) {
    String orderLabel = (String) payload.get("order_label");
    shippingService.updateShipmentStatus(orderLabel);
    return ResponseEntity.ok(BaseResponse.success("Webhook processed"));
}
```

---

## 💡 Best Practices

### 1. Tạo Shipment Sau Khi Sản Xuất Xong

```java
// Artisan hoàn thành sản xuất
customOrder.setStatus(CustomOrderStatus.READY_TO_SHIP);

// Tạo shipment
CreateShipmentRequest shipmentRequest = new CreateShipmentRequest();
// ... fill data
shippingService.createShipment(shipmentRequest);
```

### 2. Tự Động Cập Nhật Status

```java
@Scheduled(fixedRate = 3600000) // Mỗi giờ
public void updateAllShipmentStatus() {
    List<Shipment> activeShipments = shipmentRepository
        .findByStatusIn(List.of(
            ShippingStatus.PICKING,
            ShippingStatus.TRANSPORTING,
            ShippingStatus.DELIVERING
        ));
    
    for (Shipment shipment : activeShipments) {
        shippingService.updateShipmentStatus(shipment.getGhtkOrderLabel());
    }
}
```

### 3. Notify Customer

```java
if (shipment.getStatus() == ShippingStatus.DELIVERED) {
    notificationService.notifyCustomerOfDelivery(
        customOrder.getRequest().getCustomer().getAccountId(),
        shipment.getTrackingNumber()
    );
}
```

---

## 🧪 Testing với Sandbox

### Test Data:

```
Địa chỉ lấy hàng (Pick):
- Province: TP. Hồ Chí Minh
- District: Quận 1
- Ward: Phường Bến Nghé

Địa chỉ giao hàng (Delivery):
- Province: TP. Hồ Chí Minh
- District: Quận 3
- Ward: Phường 1
```

### Test Flow:

1. Tạo đơn vận chuyển
2. Check tracking number
3. Simulate status changes (via GHTK dashboard)
4. Verify webhook callbacks
5. Check final status

---

## ⚠️ Error Handling

### Common Errors:

```java
// Invalid address
catch (BadRequestException e) {
    // Địa chỉ không hợp lệ
    // Yêu cầu customer cập nhật địa chỉ
}

// GHTK API down
catch (Exception e) {
    // Retry sau 5 phút
    // Hoặc chuyển sang manual shipping
}

// Cannot cancel
if (shipment.getStatus() == ShippingStatus.DELIVERING) {
    throw new BadRequestException("Không thể hủy đơn đang giao");
}
```

---

## 📝 Database Schema

```sql
CREATE TABLE shipments (
    shipment_id UUID PRIMARY KEY,
    order_id UUID REFERENCES orders(order_id),
    custom_order_id UUID REFERENCES custom_orders(custom_order_id),
    ghtk_order_label VARCHAR(50) UNIQUE,
    tracking_number VARCHAR(50) UNIQUE,
    status VARCHAR(20) NOT NULL,
    pick_address TEXT,
    delivery_address TEXT,
    recipient_name VARCHAR(100),
    recipient_phone VARCHAR(20),
    shipping_fee DECIMAL(18,2),
    insurance_fee DECIMAL(18,2),
    estimated_delivery TIMESTAMP,
    actual_delivery TIMESTAMP,
    note TEXT,
    status_history TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

---

## 🎓 Next Steps

1. ✅ Setup GHTK sandbox account
2. ✅ Configure application.yml
3. ✅ Test create shipment API
4. ✅ Test tracking API
5. ✅ Setup webhook endpoint
6. ✅ Integrate with Order/CustomOrder flow
7. ✅ Add notification for status changes
8. ✅ Add scheduled job for auto-update

---

Generated: 2026-03-28
