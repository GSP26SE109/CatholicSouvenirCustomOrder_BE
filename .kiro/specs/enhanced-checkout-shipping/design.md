# Design Document - Enhanced Checkout with Shipping Fee Calculation

## Overview

This design enhances the checkout flow by integrating GHN shipping fee calculation before order creation, storing complete shipping information in Order entities, and enabling artisans to create shipments with pre-filled data. The solution maintains backward compatibility with existing payment and shipping flows while adding new capabilities for shipping fee preview and address management.

## Architecture

### High-Level Flow

```
┌─────────────┐
│   Customer  │
└──────┬──────┘
       │
       │ 1. Get address data
       ▼
┌─────────────────────────────────┐
│  GHN Address Service            │
│  - GET /provinces               │
│  - GET /districts?provinceId    │
│  - GET /wards?districtId        │
└─────────────────────────────────┘
       │
       │ 2. Calculate shipping fee
       ▼
┌─────────────────────────────────┐
│  Checkout Service               │
│  - GET /calculate-shipping      │
│  - Returns fee breakdown        │
└─────────────────────────────────┘
       │
       │ 3. Checkout with shipping info
       ▼
┌─────────────────────────────────┐
│  Checkout Service               │
│  - POST /checkout               │
│  - Creates OrderGroup + Orders  │
│  - Stores shipping info         │
└─────────────────────────────────┘
       │
       │ 4. Payment
       ▼
┌─────────────────────────────────┐
│  Payment Service                │
│  - POST /initiate               │
│  - VNPay payment flow           │
└─────────────────────────────────┘
       │
       │ 5. After payment success
       ▼
┌─────────────────────────────────┐
│  Shipping Service (Artisan)     │
│  - POST /shipments              │
│  - Uses stored shipping info    │
└─────────────────────────────────┘
```

### Component Interaction

```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│   Frontend   │─────▶│   Backend    │─────▶│   GHN API    │
│              │      │              │      │              │
│ - Address UI │      │ - Checkout   │      │ - Address    │
│ - Fee calc   │      │ - Shipping   │      │ - Fee calc   │
│ - Checkout   │      │ - Payment    │      │ - Shipment   │
└──────────────┘      └──────────────┘      └──────────────┘
                             │
                             ▼
                      ┌──────────────┐
                      │   Database   │
                      │              │
                      │ - Order      │
                      │ - OrderGroup │
                      │ - Shipment   │
                      └──────────────┘
```

## Components and Interfaces

### 1. Enhanced CheckoutRequest DTO

**Purpose:** Accept shipping information during checkout

**Location:** `dto/request/CheckoutRequest.java`

**Changes:**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    
    // Existing fields
    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod; // VNPAY, ZALOPAY, COD
    
    // NEW: Shipping information fields
    @NotBlank(message = "Tên người nhận không được để trống")
    private String recipientName;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại không hợp lệ")
    private String recipientPhone;
    
    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    private String deliveryAddress;
    
    @NotNull(message = "District ID không được để trống")
    private Integer toDistrictId;
    
    @NotBlank(message = "Ward code không được để trống")
    private String toWardCode;
    
    // Optional: Province/District/Ward names for display
    private String provinceName;
    private String districtName;
    private String wardName;
    
    // Optional: Notes for delivery
    private String notes;
    
    // Optional: Package dimensions (use defaults if not provided)
    @Builder.Default
    private Integer weight = 1000; // grams
    @Builder.Default
    private Integer length = 20; // cm
    @Builder.Default
    private Integer width = 20; // cm
    @Builder.Default
    private Integer height = 10; // cm
}
```

### 2. ShippingFeeCalculationRequest DTO

**Purpose:** Request shipping fee calculation before checkout

**Location:** `dto/request/ShippingFeeCalculationRequest.java`

**New DTO:**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingFeeCalculationRequest {
    
    @NotNull(message = "District ID không được để trống")
    private Integer toDistrictId;
    
    @NotBlank(message = "Ward code không được để trống")
    private String toWardCode;
    
    // Optional: Override default dimensions
    private Integer weight;
    private Integer length;
    private Integer width;
    private Integer height;
    
    @Builder.Default
    private Integer serviceTypeId = 2; // GHN service type
}
```

### 3. ShippingFeeResponse DTO

**Purpose:** Return shipping fee breakdown

**Location:** `dto/response/ShippingFeeResponse.java`

**New DTO:**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingFeeResponse {
    
    // Total shipping fee for all artisans
    private BigDecimal totalShippingFee;
    
    // Breakdown by artisan
    private List<ArtisanShippingFee> artisanFees;
    
    // Cart summary
    private BigDecimal productTotal;
    private BigDecimal grandTotal; // productTotal + totalShippingFee
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArtisanShippingFee {
        private UUID artisanId;
        private String artisanName;
        private BigDecimal shippingFee;
        private Integer itemCount;
        private BigDecimal subtotal; // Product subtotal for this artisan
    }
}
```

### 4. Enhanced Order Entity

**Purpose:** Store shipping information in Order

**Location:** `model/Order.java`

**Changes:**
```java
@Entity
@Data
@Table(name = "orders")
public class Order {
    // Existing fields...
    
    // NEW: Shipping information fields
    @Column(length = 255)
    private String recipientName;
    
    @Column(length = 20)
    private String recipientPhone;
    
    @Column(columnDefinition = "TEXT")
    private String deliveryAddress;
    
    @Column(name = "to_district_id")
    private Integer toDistrictId;
    
    @Column(length = 20, name = "to_ward_code")
    private String toWardCode;
    
    // Optional: Display names
    @Column(length = 100)
    private String provinceName;
    
    @Column(length = 100)
    private String districtName;
    
    @Column(length = 100)
    private String wardName;
    
    // Shipping fee for this order
    @Column(precision = 10, scale = 2)
    private BigDecimal shippingFee;
    
    // Package dimensions
    private Integer packageWeight; // grams
    private Integer packageLength; // cm
    private Integer packageWidth; // cm
    private Integer packageHeight; // cm
    
    // Delivery notes
    @Column(columnDefinition = "TEXT")
    private String deliveryNotes;
}
```

### 5. Enhanced CheckoutService

**Purpose:** Add shipping fee calculation and store shipping info

**Location:** `service/CheckoutService.java` and `service/imp/CheckoutServiceImp.java`

**New Methods:**
```java
public interface CheckoutService {
    // Existing method
    CheckoutResponseDTO checkout(UUID customerId, CheckoutRequest request);
    void validateCart(UUID customerId);
    
    // NEW: Calculate shipping fee before checkout
    ShippingFeeResponse calculateShippingFee(UUID customerId, ShippingFeeCalculationRequest request);
    
    // NEW: Get customer's saved address from UserProfile
    CheckoutRequest getCustomerDefaultAddress(UUID customerId);
}
```

**Implementation Details:**

```java
@Override
public ShippingFeeResponse calculateShippingFee(UUID customerId, ShippingFeeCalculationRequest request) {
    // 1. Get customer's cart
    Cart cart = cartRepository.findByCustomer_AccountId(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giỏ hàng"));
    
    if (cart.getItems().isEmpty()) {
        throw new BadRequestException("Giỏ hàng trống");
    }
    
    // 2. Group items by artisan
    Map<UUID, List<CartItem>> itemsByArtisan = cart.getItems().stream()
            .collect(Collectors.groupingBy(item -> {
                if (item.getType() == CartItemType.PRODUCT) {
                    return item.getProduct().getArtisan().getArtisanUuid();
                } else {
                    return item.getTemplate().getArtisan().getArtisanUuid();
                }
            }));
    
    // 3. Calculate shipping fee for each artisan
    List<ShippingFeeResponse.ArtisanShippingFee> artisanFees = new ArrayList<>();
    BigDecimal totalShippingFee = BigDecimal.ZERO;
    BigDecimal productTotal = BigDecimal.ZERO;
    
    for (Map.Entry<UUID, List<CartItem>> entry : itemsByArtisan.entrySet()) {
        UUID artisanId = entry.getKey();
        List<CartItem> items = entry.getValue();
        
        // Calculate subtotal for this artisan
        BigDecimal subtotal = items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        productTotal = productTotal.add(subtotal);
        
        // Calculate weight (sum of all items, or use default)
        Integer totalWeight = request.getWeight() != null ? request.getWeight() : 1000;
        
        // Build GHN request
        CreateShipmentRequest shipmentRequest = CreateShipmentRequest.builder()
                .toDistrictId(request.getToDistrictId())
                .toWardCode(request.getToWardCode())
                .orderValue(subtotal)
                .weight(totalWeight)
                .length(request.getLength() != null ? request.getLength() : 20)
                .width(request.getWidth() != null ? request.getWidth() : 20)
                .height(request.getHeight() != null ? request.getHeight() : 10)
                .serviceTypeId(request.getServiceTypeId())
                .build();
        
        // Call GHN API
        BigDecimal shippingFee = shippingService.calculateShippingFee(shipmentRequest);
        totalShippingFee = totalShippingFee.add(shippingFee);
        
        // Get artisan name
        Artisan artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nghệ nhân"));
        
        artisanFees.add(ShippingFeeResponse.ArtisanShippingFee.builder()
                .artisanId(artisanId)
                .artisanName(artisan.getArtisanName())
                .shippingFee(shippingFee)
                .itemCount(items.size())
                .subtotal(subtotal)
                .build());
    }
    
    return ShippingFeeResponse.builder()
            .totalShippingFee(totalShippingFee)
            .artisanFees(artisanFees)
            .productTotal(productTotal)
            .grandTotal(productTotal.add(totalShippingFee))
            .build();
}

@Override
public CheckoutRequest getCustomerDefaultAddress(UUID customerId) {
    UserProfile profile = userProfileRepository.findByAccount_AccountId(customerId)
            .orElse(null);
    
    if (profile == null) {
        return CheckoutRequest.builder().build();
    }
    
    // Map UserProfile address to CheckoutRequest
    CheckoutRequest request = CheckoutRequest.builder()
            .recipientName(profile.getFullName())
            .recipientPhone(profile.getPhone())
            .deliveryAddress(profile.getAddress())
            .provinceName(profile.getCity())
            .districtName(profile.getDistrict())
            .wardName(profile.getWard())
            .build();
    
    // If district/ward names exist, try to find their IDs
    if (profile.getCity() != null && profile.getDistrict() != null) {
        try {
            // Search for district ID
            Map<String, Object> district = ghnAddressService.searchDistrict(
                    getProvinceIdByName(profile.getCity()), 
                    profile.getDistrict()
            );
            if (district != null) {
                request.setToDistrictId((Integer) district.get("DistrictID"));
            }
            
            // Search for ward code
            if (request.getToDistrictId() != null && profile.getWard() != null) {
                Map<String, Object> ward = ghnAddressService.searchWard(
                        request.getToDistrictId(), 
                        profile.getWard()
                );
                if (ward != null) {
                    request.setToWardCode((String) ward.get("WardCode"));
                }
            }
        } catch (Exception e) {
            log.warn("Could not map address to GHN IDs: {}", e.getMessage());
        }
    }
    
    return request;
}
```

### 6. Enhanced CheckoutController

**Purpose:** Add new endpoints for shipping fee calculation

**Location:** `controller/CheckoutController.java`

**New Endpoints:**
```java
@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {
    
    private final CheckoutService checkoutService;
    
    // Existing endpoint
    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<CheckoutResponseDTO>> checkout(
            @AuthenticationPrincipal UUID customerId,
            @Valid @RequestBody CheckoutRequest request) {
        // Enhanced to store shipping info
        CheckoutResponseDTO response = checkoutService.checkout(customerId, request);
        return ResponseEntity.ok(BaseResponse.<CheckoutResponseDTO>builder()
                .code(200)
                .message(response.getMessage())
                .data(response)
                .build());
    }
    
    // NEW: Calculate shipping fee
    @PostMapping("/calculate-shipping")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<ShippingFeeResponse>> calculateShippingFee(
            @AuthenticationPrincipal UUID customerId,
            @Valid @RequestBody ShippingFeeCalculationRequest request) {
        
        ShippingFeeResponse response = checkoutService.calculateShippingFee(customerId, request);
        
        return ResponseEntity.ok(BaseResponse.<ShippingFeeResponse>builder()
                .code(200)
                .message("Tính phí vận chuyển thành công")
                .data(response)
                .build());
    }
    
    // NEW: Get customer's default address
    @GetMapping("/default-address")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<CheckoutRequest>> getDefaultAddress(
            @AuthenticationPrincipal UUID customerId) {
        
        CheckoutRequest address = checkoutService.getCustomerDefaultAddress(customerId);
        
        return ResponseEntity.ok(BaseResponse.<CheckoutRequest>builder()
                .code(200)
                .message("Lấy địa chỉ mặc định thành công")
                .data(address)
                .build());
    }
    
    // Existing validate endpoint
    @GetMapping("/validate")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<String>> validateCart(
            @AuthenticationPrincipal UUID customerId) {
        checkoutService.validateCart(customerId);
        return ResponseEntity.ok(BaseResponse.<String>builder()
                .code(200)
                .message("Giỏ hàng hợp lệ")
                .data("OK")
                .build());
    }
}
```

## Data Models

### Database Schema Changes

**Migration Script:**
```sql
-- Add shipping information columns to orders table
ALTER TABLE orders 
ADD COLUMN recipient_name VARCHAR(255),
ADD COLUMN recipient_phone VARCHAR(20),
ADD COLUMN delivery_address TEXT,
ADD COLUMN to_district_id INTEGER,
ADD COLUMN to_ward_code VARCHAR(20),
ADD COLUMN province_name VARCHAR(100),
ADD COLUMN district_name VARCHAR(100),
ADD COLUMN ward_name VARCHAR(100),
ADD COLUMN shipping_fee DECIMAL(10, 2),
ADD COLUMN package_weight INTEGER,
ADD COLUMN package_length INTEGER,
ADD COLUMN package_width INTEGER,
ADD COLUMN package_height INTEGER,
ADD COLUMN delivery_notes TEXT;

-- Add indexes for performance
CREATE INDEX idx_orders_to_district ON orders(to_district_id);
CREATE INDEX idx_orders_to_ward ON orders(to_ward_code);
```

## Error Handling

### Error Scenarios

1. **GHN API Failure**
   - Fallback to default shipping fee (30,000 VND per artisan)
   - Log error for monitoring
   - Return response with default fee and warning message

2. **Invalid Address**
   - Validate district ID and ward code exist in GHN
   - Return clear error message to user
   - Suggest using address selection dropdowns

3. **Empty Cart**
   - Return error before calling GHN API
   - Message: "Giỏ hàng trống"

4. **Missing Shipping Info**
   - Validate all required fields in CheckoutRequest
   - Return validation errors with field names

### Error Response Format

```json
{
  "code": 400,
  "message": "Validation failed",
  "data": null,
  "errors": [
    {
      "field": "toDistrictId",
      "message": "District ID không được để trống"
    },
    {
      "field": "recipientPhone",
      "message": "Số điện thoại không hợp lệ"
    }
  ]
}
```

## Testing Strategy

### Unit Tests

1. **CheckoutServiceImp.calculateShippingFee()**
   - Test with single artisan cart
   - Test with multiple artisans
   - Test GHN API failure fallback
   - Test empty cart handling

2. **CheckoutServiceImp.getCustomerDefaultAddress()**
   - Test with complete UserProfile
   - Test with partial UserProfile
   - Test with no UserProfile

3. **CheckoutServiceImp.checkout() - Enhanced**
   - Test shipping info storage in Order
   - Test shipping fee inclusion in OrderGroup total
   - Test with multiple artisans

### Integration Tests

1. **Checkout Flow End-to-End**
   - Calculate shipping fee
   - Checkout with shipping info
   - Verify Order contains shipping data
   - Verify OrderGroup total includes shipping
   - Initiate payment
   - Verify payment amount matches

2. **GHN Integration**
   - Test address API calls
   - Test fee calculation API
   - Test with real GHN sandbox credentials

### API Testing

**Test Scenarios:**

1. **Calculate Shipping Fee**
```bash
POST /api/checkout/calculate-shipping
Authorization: Bearer {token}
{
  "toDistrictId": 1442,
  "toWardCode": "21211"
}

Expected Response:
{
  "code": 200,
  "message": "Tính phí vận chuyển thành công",
  "data": {
    "totalShippingFee": 45000,
    "artisanFees": [
      {
        "artisanId": "uuid",
        "artisanName": "Artisan Name",
        "shippingFee": 45000,
        "itemCount": 2,
        "subtotal": 120000
      }
    ],
    "productTotal": 120000,
    "grandTotal": 165000
  }
}
```

2. **Checkout with Shipping Info**
```bash
POST /api/checkout
Authorization: Bearer {token}
{
  "paymentMethod": "VNPAY",
  "recipientName": "Nguyen Van A",
  "recipientPhone": "0901234567",
  "deliveryAddress": "123 Nguyen Hue",
  "toDistrictId": 1442,
  "toWardCode": "21211",
  "provinceName": "TP. Hồ Chí Minh",
  "districtName": "Quận 1",
  "wardName": "Phường Bến Nghé",
  "notes": "Giao giờ hành chính"
}

Expected Response:
{
  "code": 200,
  "message": "Đã tạo 1 đơn hàng thành công...",
  "data": {
    "orderGroupId": "uuid",
    "orders": [...],
    "totalAmount": 165000,  // Includes shipping
    "orderCount": 1
  }
}
```

## Performance Considerations

1. **Caching GHN Address Data**
   - Cache provinces/districts/wards in Redis
   - TTL: 24 hours
   - Reduces GHN API calls

2. **Async Shipping Fee Calculation**
   - For multiple artisans, calculate fees in parallel
   - Use CompletableFuture for concurrent GHN calls

3. **Database Indexing**
   - Index on toDistrictId and toWardCode for reporting
   - Index on orderGroupId for payment queries

## Security Considerations

1. **Input Validation**
   - Validate district ID and ward code format
   - Sanitize delivery address input
   - Validate phone number format

2. **Authorization**
   - Customer can only calculate shipping for their own cart
   - Customer can only checkout their own cart
   - Artisan can only create shipments for their own orders

3. **Data Privacy**
   - Mask phone numbers in logs
   - Encrypt sensitive shipping information at rest

## Frontend Integration Guide

### Step-by-Step Implementation

**1. Load Address Dropdowns**
```javascript
// Get provinces
const provinces = await fetch('/api/shipments/address/provinces');

// Get districts when province selected
const districts = await fetch(`/api/shipments/address/districts?provinceId=${provinceId}`);

// Get wards when district selected
const wards = await fetch(`/api/shipments/address/wards?districtId=${districtId}`);
```

**2. Calculate Shipping Fee (Real-time)**
```javascript
// When user selects ward, calculate shipping
const response = await fetch('/api/checkout/calculate-shipping', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    toDistrictId: selectedDistrictId,
    toWardCode: selectedWardCode
  })
});

const { data } = await response.json();
// Display: data.totalShippingFee, data.grandTotal
```

**3. Checkout with Shipping Info**
```javascript
const checkoutResponse = await fetch('/api/checkout', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    paymentMethod: 'VNPAY',
    recipientName: form.recipientName,
    recipientPhone: form.recipientPhone,
    deliveryAddress: form.deliveryAddress,
    toDistrictId: form.toDistrictId,
    toWardCode: form.toWardCode,
    provinceName: form.provinceName,
    districtName: form.districtName,
    wardName: form.wardName,
    notes: form.notes
  })
});

const { data: checkoutData } = await checkoutResponse.json();
const orderGroupId = checkoutData.orderGroupId;
```

**4. Initiate Payment**
```javascript
const paymentResponse = await fetch('/api/payments/initiate', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    orderGroupId: orderGroupId,
    method: 'VNPAY',
    returnUrl: 'https://yourfrontend.com/payment/result'
  })
});

const { data: paymentData } = await paymentResponse.json();
// Redirect to: paymentData.paymentUrl
window.location.href = paymentData.paymentUrl;
```

### UI/UX Recommendations

1. **Address Selection**
   - Use cascading dropdowns (Province → District → Ward)
   - Show loading state while fetching data
   - Disable next dropdown until previous is selected

2. **Shipping Fee Display**
   - Show "Calculating..." while API call in progress
   - Display breakdown by artisan if multiple
   - Highlight total amount clearly

3. **Form Validation**
   - Validate phone number format (10-11 digits)
   - Require all address fields
   - Show inline validation errors

4. **Order Summary**
```
Tóm tắt đơn hàng
─────────────────────────────
Sản phẩm (2)         120,000₫
Phí vận chuyển        45,000₫
─────────────────────────────
Tổng cộng            165,000₫
```

## Migration Plan

### Phase 1: Database Migration
1. Run ALTER TABLE script on staging
2. Test with existing orders (NULL values allowed)
3. Deploy to production during low-traffic window

### Phase 2: Backend Deployment
1. Deploy new DTOs and entities
2. Deploy enhanced CheckoutService
3. Deploy new controller endpoints
4. Monitor logs for errors

### Phase 3: Frontend Integration
1. Update checkout page with address selection
2. Integrate shipping fee calculation
3. Update checkout API call with new fields
4. Test end-to-end flow

### Phase 4: Artisan Shipment Creation
1. Update artisan dashboard to show shipping info
2. Pre-fill CreateShipmentRequest from Order data
3. Test shipment creation flow

## Rollback Plan

If issues occur:
1. Frontend can continue using old checkout (without shipping info)
2. Backend will accept NULL shipping fields
3. Artisans can manually enter shipping info as before
4. Database columns can remain (no data loss)

## Monitoring and Logging

### Key Metrics

1. **Shipping Fee Calculation**
   - Success rate of GHN API calls
   - Average response time
   - Fallback usage rate

2. **Checkout Success Rate**
   - Orders with complete shipping info
   - Orders with missing shipping info
   - Validation error rate

3. **Payment Flow**
   - Orders with shipping fee vs without
   - Average order value change

### Log Points

```java
log.info("Calculating shipping fee for customer: {}, district: {}, ward: {}", 
         customerId, districtId, wardCode);
log.info("Shipping fee calculated: {} VND for {} artisans", totalFee, artisanCount);
log.info("Checkout completed with shipping info: orderId={}, shippingFee={}", 
         orderId, shippingFee);
log.warn("GHN API failed, using default shipping fee: {}", defaultFee);
log.error("Failed to calculate shipping fee: {}", e.getMessage());
```

## Future Enhancements

1. **Multiple Delivery Addresses**
   - Allow customers to save multiple addresses
   - Quick select from saved addresses

2. **Shipping Fee Optimization**
   - Compare multiple shipping services
   - Suggest cheapest option

3. **Delivery Time Estimation**
   - Show estimated delivery date from GHN
   - Allow customer to choose delivery speed

4. **Address Validation**
   - Validate address format with GHN
   - Suggest corrections for invalid addresses

5. **Shipping Insurance**
   - Optional insurance for high-value orders
   - Calculate insurance fee

## Conclusion

This design provides a complete solution for integrating shipping fee calculation into the checkout flow while maintaining compatibility with existing payment and shipping systems. The phased approach allows for safe deployment and easy rollback if needed.
