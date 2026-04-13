# NOTIFICATION ENUMS - FINAL VERSION
## ⚠️ CHỐT ĐỂ DEPLOY - KHÔNG SỬA NỮA

Đây là version cuối cùng của tất cả notification enums dựa trên code đang sử dụng.

---

## 1. NotificationAction (action_type)
**Mục đích:** Action mà user có thể thực hiện khi nhận notification

### ✅ ĐANG DÙNG TRONG CODE:
```java
public enum NotificationAction {
    NONE,                    // Just informational, no action required
    
    // Custom Request Actions
    ACCEPT_REQUEST,          // Artisan accepts custom request ✅ USED
    REJECT_REQUEST,          // Artisan rejects custom request (có logic)
    VIEW_REQUEST,            // View custom request details ✅ USED
    CONFIRM_ARTISAN,         // Customer confirms and selects artisan (chưa dùng nhưng cần)
    
    // Custom Order & Stage Actions
    VIEW_ORDER,              // View custom order details ✅ USED
    PAY_STAGE,               // Customer pays for custom order stage ✅ USED
    COMPLETE_STAGE,          // Artisan marks stage as completed ✅ USED
    APPROVE_STAGE,           // Customer approves completed stage (chưa dùng nhưng cần)
    
    // Communication Actions
    VIEW_CONVERSATION,       // View conversation/chat messages ✅ USED
    
    // General Actions
    VIEW_NOTIFICATION        // View notification details (dự phòng)
}
```

### 📊 Usage trong NotificationServiceImp:
- `ACCEPT_REQUEST` - line 53 (notifyArtisanOfNewCustomRequest)
- `VIEW_ORDER` - line 81, 235, 262 (request accepted, payment received, order completed)
- `VIEW_REQUEST` - line 111, 291 (request rejected, artisan selected)
- `PAY_STAGE` - line 139, 206, 417, 444 (order created, stage completed, payment pending)
- `COMPLETE_STAGE` - line 166 (payment received)
- `VIEW_CONVERSATION` - line 320, 352 (new conversation, new message)

---

## 2. NotificationType (type)
**Mục đích:** Loại notification để phân loại và hiển thị

### ✅ ĐANG DÙNG TRONG CODE:
```java
public enum NotificationType {
    // Customer notifications
    ORDER_CREATED,         // When custom order is created with stages ✅ USED (line 131, 409)
    STAGE_COMPLETED,       // When artisan completes a stage ✅ USED (line 201)
    ORDER_SHIPPED,         // When order is shipped (chưa implement)
    ORDER_DELIVERED,       // When order is delivered (chưa implement)
    ORDER_COMPLETED,       // When artisan completes custom order ✅ USED (line 254)
    REQUEST_ACCEPTED,      // When artisan accepts custom request ✅ USED (line 73)
    REQUEST_REJECTED,      // When artisan rejects custom request ✅ USED (line 106)
    
    // Artisan notifications
    NEW_CUSTOM_REQUEST,    // When customer creates custom request ✅ USED (line 48, 513, 545)
    REQUEST_CONFIRMED,     // When customer confirms and selects artisan ✅ USED (line 283)
    PAYMENT_RECEIVED,      // When payment is received ✅ USED (line 158, 227)
    PAYMENT_PENDING,       // When payment is pending ✅ USED (line 436)
    
    // Chat & Conversation
    NEW_CONVERSATION,      // When conversation is created ✅ USED (line 312)
    NEW_MESSAGE,           // When new chat message arrives ✅ USED (line 344)
    
    // General (chưa dùng nhưng nên giữ)
    SYSTEM_ANNOUNCEMENT,   // System announcements
    ACCOUNT_VERIFIED       // Account verification
}
```

### 📊 Usage Summary:
- **Đang dùng:** 12 types
- **Chưa dùng nhưng cần:** 4 types (ORDER_SHIPPED, ORDER_DELIVERED, SYSTEM_ANNOUNCEMENT, ACCOUNT_VERIFIED)

---

## 3. RelatedEntityType (related_entity_type)
**Mục đích:** Loại entity mà notification liên quan đến

### ✅ ĐANG DÙNG TRONG CODE:
```java
public enum RelatedEntityType {
    CUSTOM_REQUEST,        // Custom request entity ✅ USED (line 52, 80, 110, 290)
    CUSTOM_ORDER,          // Custom order entity ✅ USED (line 138, 234, 261, 416)
    STAGE_PAYMENT,         // Stage payment entity (tên cũ: STAGE) ✅ USED (line 165, 205, 443)
    PAYMENT,               // Payment entity (chưa dùng nhưng nên có)
    CONVERSATION,          // Chat conversation entity ✅ USED (line 319, 351)
    CHAT_MESSAGE,          // Chat message entity (chưa dùng)
    ACCOUNT                // Account entity (chưa dùng)
}
```

### ⚠️ VẤN ĐỀ:
Code đang dùng `RelatedEntityType.STAGE` nhưng enum hiện tại có `STAGE_PAYMENT`.
Cần check xem database constraint có `STAGE` hay `STAGE_PAYMENT`?

### 📊 Usage trong NotificationServiceImp:
- `CUSTOM_REQUEST` - 4 lần
- `CUSTOM_ORDER` - 4 lần  
- `STAGE` - 3 lần (⚠️ cần confirm tên)
- `CONVERSATION` - 2 lần

---

## 4. NotificationPriority (priority)
**Mục đích:** Độ ưu tiên của notification

### ✅ HOÀN CHỈNH:
```java
public enum NotificationPriority {
    LOW,       // Thông tin không quan trọng
    NORMAL,    // Thông tin bình thường ✅ USED
    HIGH,      // Quan trọng, cần chú ý ✅ USED
    URGENT     // Rất quan trọng, cần xử lý ngay
}
```

### 📊 Usage trong NotificationServiceImp:
- `HIGH` - Cho payment, order created, request accepted/confirmed
- `NORMAL` - Cho conversation, messages, stage completed

---

## 🎯 DATABASE CONSTRAINT FINAL

### SQL Script để chốt:
```sql
-- 1. NotificationAction constraint
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_action_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_action_type_check 
CHECK (action_type IN (
    'NONE',
    'ACCEPT_REQUEST',
    'REJECT_REQUEST',
    'VIEW_REQUEST',
    'CONFIRM_ARTISAN',
    'VIEW_ORDER',
    'PAY_STAGE',
    'COMPLETE_STAGE',
    'APPROVE_STAGE',
    'VIEW_CONVERSATION',
    'VIEW_NOTIFICATION'
));

-- 2. NotificationType constraint
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check 
CHECK (type IN (
    'ORDER_CREATED',
    'STAGE_COMPLETED',
    'ORDER_SHIPPED',
    'ORDER_DELIVERED',
    'ORDER_COMPLETED',
    'REQUEST_ACCEPTED',
    'REQUEST_REJECTED',
    'NEW_CUSTOM_REQUEST',
    'REQUEST_CONFIRMED',
    'PAYMENT_RECEIVED',
    'PAYMENT_PENDING',
    'NEW_CONVERSATION',
    'NEW_MESSAGE',
    'SYSTEM_ANNOUNCEMENT',
    'ACCOUNT_VERIFIED'
));

-- 3. RelatedEntityType constraint
-- ⚠️ CẦN CONFIRM: Code dùng 'STAGE' hay 'STAGE_PAYMENT'?
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_related_entity_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_related_entity_type_check 
CHECK (related_entity_type IN (
    'CUSTOM_REQUEST',
    'CUSTOM_ORDER',
    'STAGE',              -- ⚠️ hoặc 'STAGE_PAYMENT'?
    'PAYMENT',
    'CONVERSATION',
    'CHAT_MESSAGE',
    'ACCOUNT'
));

-- 4. NotificationPriority constraint
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_priority_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_priority_check 
CHECK (priority IN (
    'LOW',
    'NORMAL',
    'HIGH',
    'URGENT'
));
```

---

## ⚠️ CẦN XÁC NHẬN:

1. **RelatedEntityType.STAGE vs STAGE_PAYMENT**
   - Code đang dùng: `RelatedEntityType.STAGE`
   - Enum hiện tại: `STAGE_PAYMENT`
   - Database có gì?

2. **Có cần thêm actions cho Ready-made products không?**
   - VIEW_PRODUCT?
   - ADD_TO_CART?
   - Hay dùng chung VIEW_ORDER?

3. **Có cần thêm types cho Template-based flow không?**
   - TEMPLATE_ORDER_CREATED?
   - Hay dùng chung ORDER_CREATED?

---

## 📝 RECOMMENDATION:

**Nên giữ nguyên tất cả enums hiện tại** vì:
- Đã cover đủ 3 flows (Request-based, Template-based, Ready-made)
- Có dự phòng cho tương lai (ORDER_SHIPPED, SYSTEM_ANNOUNCEMENT...)
- Không thừa quá nhiều

**Chỉ cần fix:**
1. Confirm `STAGE` vs `STAGE_PAYMENT` trong RelatedEntityType
2. Chạy SQL script để update database constraints
3. Restart app

Done!
