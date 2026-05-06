# Inventory Reservation System

## 📋 Tổng quan
Hệ thống quản lý tồn kho với reservation mechanism để đảm bảo:
- ✅ Không bán quá số lượng có sẵn
- ✅ Reserve stock khi checkout (chưa thanh toán)
- ✅ Confirm reservation khi payment SUCCESS
- ✅ Release reservation khi payment FAILED hoặc timeout

## 🏗️ Kiến trúc

### 1. Product Model
```java
private int quantity;              // Tổng số lượng trong kho
private int reservedQuantity = 0;  // Số lượng đang được reserve
```

**Available Stock** = `quantity - reservedQuantity`

### 2. Luồng hoạt động

#### A. Add to Cart
```
Customer add product → Check available stock → Add to cart
```
- Validate: `product.getAvailableQuantity() >= requestedQuantity`
- Không reserve ở bước này

#### B. Checkout
```
Customer checkout → Validate stock → Reserve stock → Create orders (PENDING)
```
- Validate: `product.hasAvailableStock(quantity)`
- Reserve: `product.reserveStock(quantity)` 
- `reservedQuantity += quantity`
- Order status: `PENDING`

#### C. Payment Success
```
Payment SUCCESS → Confirm reservation → Deduct actual stock
```
- Confirm: `product.confirmReservation(quantity)`
- `quantity -= quantity`
- `reservedQuantity -= quantity`
- Order status: `PAID`

#### D. Payment Failed
```
Payment FAILED → Release reservation → Stock available again
```
- Release: `product.releaseReservation(quantity)`
- `reservedQuantity -= quantity`
- Order status: `FAILED`

#### E. Payment Timeout (Auto)
```
Scheduler (every 5 min) → Find PENDING orders > 30 min → Release reservations
```
- Auto-release sau 30 phút không thanh toán
- Order status: `EXPIRED`

## 📁 Files Changed

### Models
- ✅ `Product.java` - Added `reservedQuantity` field + helper methods

### Services
- ✅ `InventoryService.java` - Interface
- ✅ `InventoryServiceImp.java` - Implementation
- ✅ `CartServiceImp.java` - Check available stock
- ✅ `CheckoutServiceImp.java` - Reserve stock instead of deduct
- ✅ `PaymentServiceImp.java` - Confirm/Release on payment result

### Scheduler
- ✅ `ReservationCleanupScheduler.java` - Auto-release expired reservations

### Repository
- ✅ `OrderGroupRepository.java` - Added query for expired orders

### Migration
- ✅ `V3__add_reserved_quantity_to_product.sql` - Database schema

## 🔧 Configuration

### Reservation Timeout
Default: **30 minutes**

Thay đổi trong `ReservationCleanupScheduler.java`:
```java
LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(30);
```

### Cleanup Frequency
Default: **Every 5 minutes**

Thay đổi cron expression:
```java
@Scheduled(cron = "0 */5 * * * *")
```

## 🧪 Testing Scenarios

### Scenario 1: Normal Purchase
1. Add product to cart (quantity=10, available=10) ✅
2. Checkout → reserved=10, available=0 ✅
3. Payment SUCCESS → quantity=0, reserved=0 ✅

### Scenario 2: Payment Failed
1. Add product to cart (quantity=10, available=10) ✅
2. Checkout → reserved=10, available=0 ✅
3. Payment FAILED → reserved=0, available=10 ✅

### Scenario 3: Payment Timeout
1. Add product to cart (quantity=10, available=10) ✅
2. Checkout → reserved=10, available=0 ✅
3. Wait 30+ minutes → Scheduler releases → available=10 ✅

### Scenario 4: Concurrent Purchases
1. User A: Add 5 units (available=10) ✅
2. User B: Add 5 units (available=10) ✅
3. User A checkout → reserved=5, available=5 ✅
4. User B checkout → reserved=10, available=0 ✅
5. User A pays → quantity=5, reserved=5, available=0 ✅
6. User B pays → quantity=0, reserved=0, available=0 ✅

## 🚀 Deployment

1. **Run migration**: Flyway auto-runs on startup
2. **Restart application**: Scheduler starts automatically
3. **Monitor logs**: Check for reservation cleanup activity

## 📊 Monitoring

### Key Metrics
- Reserved quantity per product
- Expired reservations released
- Failed reservation confirmations

### Log Messages
- `📦 Confirming inventory reservations`
- `✅ Inventory reservations confirmed`
- `📦 Releasing inventory reservations`
- `🔄 Starting expired reservation cleanup job`

## ⚠️ Important Notes

1. **Optimistic Locking**: Product has `@Version` for concurrent updates
2. **Transaction Safety**: All inventory operations are `@Transactional`
3. **Idempotency**: Reservation methods are safe to call multiple times
4. **Scheduler**: Runs in background, no manual intervention needed
5. **Database Constraint**: `reserved_quantity <= quantity` enforced at DB level

## 🔄 Future Enhancements

- [ ] Admin dashboard to view reserved quantities
- [ ] Manual release reservation endpoint for admins
- [ ] Notification to customers when reservation expires
- [ ] Configurable timeout per product category
- [ ] Reservation history tracking
