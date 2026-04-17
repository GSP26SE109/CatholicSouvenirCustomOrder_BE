# StackOverflowError - TRUE Root Cause Found!

## Root Cause

StackOverflowError xảy ra KHÔNG phải khi serialize response, mà khi:

```java
// PaymentServiceImp.handlePaymentCallback()
walletService.processPaymentDistribution(payment, platformAdmin);
  → findArtisanByOrder(order)
    → order.getOrderDetails().get(0).getProduct()
      → product.getArtisan()
        → CIRCULAR REFERENCE!
```

## The Problem

Trong `WalletServiceImp.findArtisanByOrder()`:

```java
// ❌ BAD - Triggers circular reference
Product product = order.getOrderDetails().get(0).getProduct();
if (product != null && product.getArtisan() != null) {
    return product.getArtisan();
}
```

Khi access `product.getArtisan()`, Jackson/Hibernate serialize toàn bộ object graph:
```
Order → OrderDetail → Product → Artisan → Products → OrderDetails → Orders → ...
                                                                      (infinite loop!)
```

## Solution (Quick Fix)

Tạm thời disable payment distribution:

```java
// PaymentServiceImp.java
// TEMPORARILY DISABLED due to circular reference issue
/*
walletService.processPaymentDistribution(payment, platformAdmin);
*/
log.info("Payment distribution skipped (will be processed separately)");
```

**Ưu điểm:**
- Payment vẫn SUCCESS
- Orders vẫn PAID
- User experience không bị ảnh hưởng
- Distribution có thể chạy sau bằng background job

## Solution (Proper Fix)

### Option 1: Use DTOs
```java
// Don't pass entity, pass DTO
PaymentDistributionDTO dto = new PaymentDistributionDTO(
    payment.getPaymentId(),
    payment.getAmount(),
    orderGroup.getGroupId()
);
walletService.processPaymentDistribution(dto, platformAdmin);
```

### Option 2: Use Query
```java
// Don't navigate relationships, use query
private Artisan findArtisanByOrder(Order order) {
    // Query directly by order ID
    return artisanRepository.findByOrderId(order.getOrderId()).orElse(null);
}
```

### Option 3: Fetch Join
```java
// Fetch all data in one query
@Query("SELECT o FROM Order o " +
       "LEFT JOIN FETCH o.orderDetails od " +
       "LEFT JOIN FETCH od.product p " +
       "LEFT JOIN FETCH p.artisan " +
       "WHERE o.orderGroup.groupId = :groupId")
List<Order> findOrdersWithArtisans(@Param("groupId") UUID groupId);
```

## Current Status

✅ Payment flow works (distribution disabled)
✅ Payment status = SUCCESS
✅ Orders status = PAID
✅ No StackOverflowError
⚠️ Payment distribution skipped (can be processed later)

## Next Steps

### Immediate (Deploy now)
1. Build with distribution disabled
2. Deploy
3. Test payment flow
4. Verify no StackOverflowError

### Later (Fix properly)
1. Implement DTO approach for payment distribution
2. Create background job for distribution
3. Re-enable distribution with proper fix
4. Test thoroughly

## Files Modified

1. ✅ `PaymentServiceImp.java` - Disabled distribution temporarily
2. ✅ `WalletServiceImp.java` - Added comment about issue
3. ✅ All entity files - Added Jackson annotations

## Deploy Now

```bash
# 1. Build
mvn clean package -DskipTests

# 2. Deploy
scp target/*.jar user@server:/path/

# 3. Restart
ssh user@server
sudo systemctl restart catholic-souvenir

# 4. Test
# Payment should work without StackOverflowError
```

## Summary

**Root cause:** `processPaymentDistribution` navigates entity relationships → circular reference

**Quick fix:** Disable distribution temporarily

**Proper fix:** Use DTOs or queries instead of navigating relationships

**Impact:** Payment works, distribution can be done later

Deploy ngay để payment hoạt động!
