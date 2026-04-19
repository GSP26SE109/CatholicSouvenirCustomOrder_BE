# HƯỚNG DẪN TRIỂN KHAI ADMIN DASHBOARD STATISTICS

## I. TỔNG QUAN

Tài liệu này mô tả chi tiết cách bổ sung các statistics còn thiếu cho Admin Dashboard.

### Hiện trạng:
- ✅ Total Orders & Revenue
- ✅ Daily Revenue Chart  
- ✅ Order Status Distribution
- ✅ Top Products

### Cần bổ sung:
- ❌ Customer Statistics
- ❌ Artisan Statistics
- ❌ CustomOrder Statistics
- ❌ Complaint Statistics
- ❌ Revenue Breakdown
- ❌ Product Analytics

---

## II. CẤU TRÚC TRIỂN KHAI

### Bước 1: Tạo DTOs mới

**File**: `src/main/java/org/example/catholicsouvenircustomorder/dto/response/Dashboard/CustomerStatistics.java`

```java
package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

public interface CustomerStatistics {
    Long getTotalCustomers();
    Long getNewCustomers();  // Trong khoảng thời gian
    Long getActiveCustomers();
}
```

**File**: `src/main/java/org/example/catholicsouvenircustomorder/dto/response/Dashboard/ArtisanStatistics.java`

```java
package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

public interface ArtisanStatistics {
    Long getTotalArtisans();
    Long getPendingArtisans();
    Long getActiveArtisans();
}
```

**File**: `src/main/java/org/example/catholicsouvenircustomorder/dto/response/Dashboard/CustomOrderStatistics.java`

```java
package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

public interface CustomOrderStatistics {
    Long getTotalRequests();
    Long getTotalOrders();
    BigDecimal getAverageOrderValue();
    Double getConversionRate();  // (orders / requests) * 100
}
```

**File**: `src/main/java/org/example/catholicsouvenircustomorder/dto/response/Dashboard/ComplaintStatistics.java`

```java
package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

public interface ComplaintStatistics {
    Long getTotalComplaints();
    Long getPendingComplaints();
    Long getApprovedComplaints();
    Long getRejectedComplaints();
    BigDecimal getTotalRefundAmount();
}
```

**File**: `src/main/java/org/example/catholicsouvenircustomorder/dto/response/Dashboard/RevenueBreakdown.java`

```java
package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

public interface RevenueBreakdown {
    BigDecimal getProductRevenue();
    BigDecimal getTemplateRevenue();
    BigDecimal getCustomRevenue();
    BigDecimal getTotalCommission();
}
```

**File**: `src/main/java/org/example/catholicsouvenircustomorder/dto/response/Dashboard/ProductAnalytics.java`

```java
package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

public interface ProductAnalytics {
    Long getTotalProducts();
    Long getPendingProducts();
    Long getApprovedProducts();
    BigDecimal getAveragePrice();
}
```

**File**: `src/main/java/org/example/catholicsouvenircustomorder/dto/response/Dashboard/TopCustomerDTO.java`

```java
package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

public interface TopCustomerDTO {
    String getCustomerId();
    String getCustomerName();
    String getEmail();
    Long getTotalOrders();
    BigDecimal getTotalSpent();
}
```

**File**: `src/main/java/org/example/catholicsouvenircustomorder/dto/response/Dashboard/TopArtisanDTO.java`

```java
package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

public interface TopArtisanDTO {
    String getArtisanId();
    String getArtisanName();
    Long getTotalOrders();
    BigDecimal getTotalRevenue();
    Double getAverageRating();
}
```

---

### Bước 2: Cập nhật DashboardResponse

**File**: `src/main/java/org/example/catholicsouvenircustomorder/dto/response/Dashboard/DashboardResponse.java`

```java
package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import lombok.Builder;
import lombok.Data;
import org.example.catholicsouvenircustomorder.model.OrderStatus;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardResponse {
    // Existing fields
    private DashboardSummary summary;
    private List<DailyRevenue> revenueChart;
    private Map<OrderStatus, Integer> orderStatus;
    private List<TopProductDTO> topProducts;
    private List<ShortStockProduct> lowStockProducts;
    
    // NEW FIELDS
    private CustomerStatistics customerStats;
    private ArtisanStatistics artisanStats;
    private CustomOrderStatistics customOrderStats;
    private ComplaintStatistics complaintStats;
    private RevenueBreakdown revenueBreakdown;
    private ProductAnalytics productAnalytics;
    private List<TopCustomerDTO> topCustomers;
    private List<TopArtisanDTO> topArtisans;
}
```

---

### Bước 3: Thêm Repository Methods

#### 3.1 AccountRepository

**File**: `src/main/java/org/example/catholicsouvenircustomorder/repository/AccountRepository.java`

Thêm các methods sau:

```java
// Customer Statistics
@Query("""
    SELECT 
        COUNT(a) as totalCustomers,
        SUM(CASE WHEN a.createdAt >= :startDate THEN 1 ELSE 0 END) as newCustomers,
        SUM(CASE WHEN EXISTS (
            SELECT 1 FROM Order o WHERE o.customer = a AND o.createAt >= :startDate
        ) THEN 1 ELSE 0 END) as activeCustomers
    FROM Account a
    WHERE a.role.name = 'CUSTOMER'
    """)
CustomerStatistics getCustomerStatistics(@Param("startDate") LocalDateTime startDate);

// Top Customers
@Query("""
    SELECT 
        a.accountId as customerId,
        a.name as customerName,
        a.email as email,
        COUNT(o) as totalOrders,
        COALESCE(SUM(o.total), 0) as totalSpent
    FROM Account a
    LEFT JOIN Order o ON o.customer = a
    WHERE a.role.name = 'CUSTOMER'
    GROUP BY a.accountId, a.name, a.email
    ORDER BY totalSpent DESC
    """)
List<TopCustomerDTO> getTopCustomers(Pageable pageable);
```

#### 3.2 ArtisanRepository

**File**: `src/main/java/org/example/catholicsouvenircustomorder/repository/ArtisanRepository.java`

```java
// Artisan Statistics
@Query("""
    SELECT 
        COUNT(a) as totalArtisans,
        SUM(CASE WHEN aa.status = 'PENDING' THEN 1 ELSE 0 END) as pendingArtisans,
        SUM(CASE WHEN a.account.status = 'ACTIVE' THEN 1 ELSE 0 END) as activeArtisans
    FROM Artisan a
    LEFT JOIN ArtisanApplication aa ON aa.account = a.account
    """)
ArtisanStatistics getArtisanStatistics();

// Top Artisans
@Query("""
    SELECT 
        a.artisanId as artisanId,
        a.account.name as artisanName,
        COUNT(DISTINCT o) as totalOrders,
        COALESCE(SUM(o.total), 0) as totalRevenue,
        COALESCE(AVG(f.rating), 0.0) as averageRating
    FROM Artisan a
    LEFT JOIN Product p ON p.artisan = a
    LEFT JOIN OrderDetail od ON od.product = p
    LEFT JOIN Order o ON od.order = o
    LEFT JOIN Feedback f ON f.artisan = a
    GROUP BY a.artisanId, a.account.name
    ORDER BY totalRevenue DESC
    """)
List<TopArtisanDTO> getTopArtisans(Pageable pageable);
```

#### 3.3 CustomRequestRepository

**File**: `src/main/java/org/example/catholicsouvenircustomorder/repository/CustomRequestRepository.java`

```java
// CustomOrder Statistics
@Query("""
    SELECT 
        COUNT(cr) as totalRequests,
        SUM(CASE WHEN co IS NOT NULL THEN 1 ELSE 0 END) as totalOrders,
        COALESCE(AVG(co.totalPrice), 0) as averageOrderValue,
        CASE WHEN COUNT(cr) > 0 
            THEN (CAST(SUM(CASE WHEN co IS NOT NULL THEN 1 ELSE 0 END) AS DOUBLE) / COUNT(cr)) * 100 
            ELSE 0 
        END as conversionRate
    FROM CustomRequest cr
    LEFT JOIN CustomOrder co ON co.request = cr
    WHERE cr.createdAt >= :startDate
    """)
CustomOrderStatistics getCustomOrderStatistics(@Param("startDate") LocalDateTime startDate);
```

#### 3.4 ComplaintRepository

**File**: `src/main/java/org/example/catholicsouvenircustomorder/repository/ComplaintRepository.java`

```java
// Complaint Statistics
@Query("""
    SELECT 
        COUNT(c) as totalComplaints,
        SUM(CASE WHEN c.status = 'PENDING_ARTISAN_RESPONSE' OR c.status = 'PENDING_ADMIN_REVIEW' THEN 1 ELSE 0 END) as pendingComplaints,
        SUM(CASE WHEN c.status = 'APPROVED' OR c.status = 'REFUNDED' THEN 1 ELSE 0 END) as approvedComplaints,
        SUM(CASE WHEN c.status = 'REJECTED' THEN 1 ELSE 0 END) as rejectedComplaints,
        COALESCE(SUM(c.refundAmount), 0) as totalRefundAmount
    FROM Complaint c
    WHERE c.createdAt >= :startDate
    """)
ComplaintStatistics getComplaintStatistics(@Param("startDate") LocalDateTime startDate);
```

#### 3.5 OrderRepository

**File**: `src/main/java/org/example/catholicsouvenircustomorder/repository/OrderRepository.java`

```java
// Revenue Breakdown
@Query("""
    SELECT 
        COALESCE(SUM(CASE WHEN od.product IS NOT NULL THEN od.price * od.quantity ELSE 0 END), 0) as productRevenue,
        COALESCE(SUM(CASE WHEN otd.productTemplate IS NOT NULL THEN otd.price * otd.quantity ELSE 0 END), 0) as templateRevenue,
        0 as customRevenue,
        COALESCE(SUM(o.total * :commissionRate), 0) as totalCommission
    FROM Order o
    LEFT JOIN OrderDetail od ON od.order = o
    LEFT JOIN OrderTemplateDetail otd ON otd.order = o
    WHERE o.createAt >= :startDate
    """)
RevenueBreakdown getRevenueBreakdown(@Param("startDate") LocalDateTime startDate, @Param("commissionRate") BigDecimal commissionRate);

// Custom Order Revenue (separate query)
@Query("""
    SELECT COALESCE(SUM(co.totalPrice), 0)
    FROM CustomOrder co
    WHERE co.createdAt >= :startDate
    """)
BigDecimal getCustomOrderRevenue(@Param("startDate") LocalDateTime startDate);
```

#### 3.6 ProductRepository

**File**: `src/main/java/org/example/catholicsouvenircustomorder/repository/ProductRepository.java`

```java
// Product Analytics
@Query("""
    SELECT 
        COUNT(p) as totalProducts,
        SUM(CASE WHEN p.status = 'PENDING' THEN 1 ELSE 0 END) as pendingProducts,
        SUM(CASE WHEN p.status = 'APPROVED' THEN 1 ELSE 0 END) as approvedProducts,
        COALESCE(AVG(p.productPrice), 0) as averagePrice
    FROM Product p
    """)
ProductAnalytics getProductAnalytics();
```

---

### Bước 4: Cập nhật DashboardService Interface

**File**: `src/main/java/org/example/catholicsouvenircustomorder/service/DashboardService.java`

```java
package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.response.Dashboard.*;
import org.example.catholicsouvenircustomorder.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DashboardService {
    // Existing methods...
    DashboardSummary getArtisanDashboardSummary(LocalDateTime start, UUID artisanId);
    List<DailyRevenue> getArtisanDailyRevenueFromDate(LocalDateTime start, UUID artisanId);
    Map<OrderStatus, Integer> getArtisanOrderStatusStatistic(UUID artisanId);
    List<TopProductDTO> getArtisanMostSoldProducts(UUID artisanId);
    List<ShortStockProduct> ArtisanfindShortStockProduct(UUID artisanId);
    DashboardResponse getArtisanDashboardInDays(UUID artisanId, int days);
    DashboardSummary getAdminDashboardSummary(LocalDateTime start);
    List<DailyRevenue> getAdminDailyRevenue(LocalDateTime start);
    Map<OrderStatus, Integer> getAdminOrderStatusStatistic();
    List<TopProductDTO> getAdminMostSoldProducts();
    DashboardResponse getAdminDashboardInDays(UUID adminId, int days);
    
    // NEW METHODS
    CustomerStatistics getCustomerStatistics(LocalDateTime startDate);
    ArtisanStatistics getArtisanStatistics();
    CustomOrderStatistics getCustomOrderStatistics(LocalDateTime startDate);
    ComplaintStatistics getComplaintStatistics(LocalDateTime startDate);
    RevenueBreakdown getRevenueBreakdown(LocalDateTime startDate);
    ProductAnalytics getProductAnalytics();
    List<TopCustomerDTO> getTopCustomers();
    List<TopArtisanDTO> getTopArtisans();
}
```

---

### Bước 5: Implement DashboardServiceImp

**File**: `src/main/java/org/example/catholicsouvenircustomorder/service/imp/DashboardServiceImp.java`

Thêm vào class:

```java
@Service
@RequiredArgsConstructor
public class DashboardServiceImp implements DashboardService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final AccountService accountService;
    
    // NEW DEPENDENCIES
    private final AccountRepository accountRepository;
    private final ArtisanRepository artisanRepository;
    private final CustomRequestRepository customRequestRepository;
    private final ComplaintRepository complaintRepository;
    private final CustomOrderRepository customOrderRepository;
    private final SystemConfigService systemConfigService;
    
    // ... existing methods ...
    
    // NEW IMPLEMENTATIONS
    
    @Override
    public CustomerStatistics getCustomerStatistics(LocalDateTime startDate) {
        return accountRepository.getCustomerStatistics(startDate);
    }
    
    @Override
    public ArtisanStatistics getArtisanStatistics() {
        return artisanRepository.getArtisanStatistics();
    }
    
    @Override
    public CustomOrderStatistics getCustomOrderStatistics(LocalDateTime startDate) {
        return customRequestRepository.getCustomOrderStatistics(startDate);
    }
    
    @Override
    public ComplaintStatistics getComplaintStatistics(LocalDateTime startDate) {
        return complaintRepository.getComplaintStatistics(startDate);
    }
    
    @Override
    public RevenueBreakdown getRevenueBreakdown(LocalDateTime startDate) {
        // Get commission rate from system config
        BigDecimal commissionRate = systemConfigService.getCommissionRate();
        
        RevenueBreakdown breakdown = orderRepository.getRevenueBreakdown(startDate, commissionRate);
        
        // Add custom order revenue
        BigDecimal customRevenue = orderRepository.getCustomOrderRevenue(startDate);
        
        // Create a wrapper to include custom revenue
        return new RevenueBreakdownImpl(
            breakdown.getProductRevenue(),
            breakdown.getTemplateRevenue(),
            customRevenue,
            breakdown.getTotalCommission()
        );
    }
    
    @Override
    public ProductAnalytics getProductAnalytics() {
        return productRepository.getProductAnalytics();
    }
    
    @Override
    public List<TopCustomerDTO> getTopCustomers() {
        return accountRepository.getTopCustomers(PageRequest.of(0, 10));
    }
    
    @Override
    public List<TopArtisanDTO> getTopArtisans() {
        return artisanRepository.getTopArtisans(PageRequest.of(0, 10));
    }
    
    @Override
    public DashboardResponse getAdminDashboardInDays(UUID adminId, int days) {
        Account admin = accountService.findAccountById(adminId);
        if (!admin.getRole().getName().equals("ADMIN")) {
            throw new UnauthorizedException("Bạn không có quyền truy cập");
        }
        LocalDateTime start = LocalDateTime.now().minusDays(days);

        return DashboardResponse.builder()
                // Existing fields
                .summary(getAdminDashboardSummary(start))
                .revenueChart(getAdminDailyRevenue(start))
                .orderStatus(getAdminOrderStatusStatistic())
                .topProducts(getAdminMostSoldProducts())
                // NEW FIELDS
                .customerStats(getCustomerStatistics(start))
                .artisanStats(getArtisanStatistics())
                .customOrderStats(getCustomOrderStatistics(start))
                .complaintStats(getComplaintStatistics(start))
                .revenueBreakdown(getRevenueBreakdown(start))
                .productAnalytics(getProductAnalytics())
                .topCustomers(getTopCustomers())
                .topArtisans(getTopArtisans())
                .build();
    }
    
    // Helper class for RevenueBreakdown
    @Data
    @AllArgsConstructor
    private static class RevenueBreakdownImpl implements RevenueBreakdown {
        private BigDecimal productRevenue;
        private BigDecimal templateRevenue;
        private BigDecimal customRevenue;
        private BigDecimal totalCommission;
    }
}
```

---

## III. TESTING

### Test API Endpoint

```bash
GET /admin/dashboard?days=30
Authorization: Bearer {admin_token}
```

### Expected Response

```json
{
  "code": 200,
  "message": "Tải dashboard thành công",
  "data": {
    "summary": {
      "totalOrders": 150,
      "totalRevenue": 45000000.00
    },
    "revenueChart": [...],
    "orderStatus": {...},
    "topProducts": [...],
    "customerStats": {
      "totalCustomers": 500,
      "newCustomers": 50,
      "activeCustomers": 200
    },
    "artisanStats": {
      "totalArtisans": 30,
      "pendingArtisans": 5,
      "activeArtisans": 25
    },
    "customOrderStats": {
      "totalRequests": 100,
      "totalOrders": 75,
      "averageOrderValue": 1500000.00,
      "conversionRate": 75.0
    },
    "complaintStats": {
      "totalComplaints": 20,
      "pendingComplaints": 5,
      "approvedComplaints": 12,
      "rejectedComplaints": 3,
      "totalRefundAmount": 5000000.00
    },
    "revenueBreakdown": {
      "productRevenue": 20000000.00,
      "templateRevenue": 15000000.00,
      "customRevenue": 10000000.00,
      "totalCommission": 4500000.00
    },
    "productAnalytics": {
      "totalProducts": 200,
      "pendingProducts": 10,
      "approvedProducts": 180,
      "averagePrice": 250000.00
    },
    "topCustomers": [...],
    "topArtisans": [...]
  }
}
```

---

## IV. CHECKLIST TRIỂN KHAI

- [ ] 1. Tạo tất cả DTO interfaces (8 files)
- [ ] 2. Cập nhật DashboardResponse
- [ ] 3. Thêm methods vào AccountRepository
- [ ] 4. Thêm methods vào ArtisanRepository
- [ ] 5. Thêm methods vào CustomRequestRepository
- [ ] 6. Thêm methods vào ComplaintRepository
- [ ] 7. Cập nhật OrderRepository
- [ ] 8. Cập nhật ProductRepository
- [ ] 9. Cập nhật DashboardService interface
- [ ] 10. Implement methods trong DashboardServiceImp
- [ ] 11. Test API endpoint
- [ ] 12. Verify data accuracy

---

## V. LƯU Ý

1. **Performance**: Các queries có thể chậm với dữ liệu lớn. Cân nhắc:
   - Thêm indexes cho các columns thường query
   - Cache kết quả với Redis
   - Pagination cho top lists

2. **Commission Rate**: Cần có SystemConfigService để lấy commission rate

3. **Data Accuracy**: Test kỹ các queries với dữ liệu thực

4. **Error Handling**: Thêm try-catch và logging phù hợp

5. **Authorization**: Đảm bảo chỉ ADMIN mới access được

---

**Tác giả**: Kiro AI Assistant  
**Ngày tạo**: 19/04/2026  
**Phiên bản**: 1.0
