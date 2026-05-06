package org.example.catholicsouvenircustomorder.repository;


import org.example.catholicsouvenircustomorder.dto.response.Dashboard.DailyRevenue;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.DashboardSummary;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.RevenueBreakdown;
import org.example.catholicsouvenircustomorder.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByCustomerAccountId(UUID customerId, Pageable pageable);
    @Query("""
    SELECT DISTINCT o
    FROM Order o
    LEFT JOIN o.orderDetails od
    LEFT JOIN od.product p
    LEFT JOIN o.templateDetails otd
    LEFT JOIN otd.template t
    WHERE p.artisan.artisanUuid = :artisanId
    OR t.artisan.artisanUuid = :artisanId
""")
    Page<Order> findOrdersByArtisanId(UUID artisanId, Pageable pageable);
    
    List<Order> findByCustomer_AccountId(UUID customerId);

    //====== artisan Dashboard======//
    @Query("""
    SELECT COUNT(DISTINCT o.orderId) AS totalOrders,
           COALESCE(SUM(od.quantity * p.productPrice), 0) + COALESCE(SUM(otd.quantity * otd.unitPrice), 0) AS totalRevenue
    FROM Order o
    LEFT JOIN o.orderDetails od
    LEFT JOIN od.product p
    LEFT JOIN o.templateDetails otd
    LEFT JOIN otd.template t
    WHERE o.createAt >= :start
    AND (p.artisan.artisanUuid = :artisanId OR t.artisan.artisanUuid = :artisanId)
""")
    DashboardSummary getSummary(LocalDateTime start, UUID artisanId);

    @Query("""
    SELECT CAST(o.createAt AS date) AS date,
           COUNT(DISTINCT o.orderId) AS orderNumber,
           COALESCE(SUM(od.quantity * p.productPrice), 0) + COALESCE(SUM(otd.quantity * otd.unitPrice), 0) AS revenue
    FROM Order o
    LEFT JOIN o.orderDetails od
    LEFT JOIN od.product p
    LEFT JOIN o.templateDetails otd
    LEFT JOIN otd.template t
    WHERE o.createAt >= :startDate
    AND (p.artisan.artisanUuid = :artisanId OR t.artisan.artisanUuid = :artisanId)
    GROUP BY CAST(o.createAt AS date)
    ORDER BY CAST(o.createAt AS date)
""")
    List<DailyRevenue> getRevenueFromDate(LocalDateTime startDate, UUID artisanId);

    @Query("""
    SELECT o.status, COUNT(DISTINCT o.orderId)
    FROM Order o
    LEFT JOIN o.orderDetails od
    LEFT JOIN od.product p
    LEFT JOIN o.templateDetails otd
    LEFT JOIN otd.template t
    WHERE p.artisan.artisanUuid = :artisanId OR t.artisan.artisanUuid = :artisanId
    GROUP BY o.status
""")
    List<Object[]> getOrderStatusRaw(UUID artisanId);

    //====== admin Dashboard======//
    @Query("""
    SELECT COUNT(DISTINCT o.orderId) AS totalOrders,
           SUM(od.quantity * p.productPrice * 0.05) AS totalRevenue
    FROM Order o
    JOIN o.orderDetails od
    JOIN od.product p
    WHERE o.createAt >= :start
""")

    DashboardSummary getAdminSummary(LocalDateTime start);
    @Query("""
    SELECT CAST(o.createAt AS date) AS date,
           COUNT(DISTINCT o.orderId) AS orderNumber,
           SUM(od.quantity * p.productPrice * 0.05) AS revenue
    FROM Order o
    JOIN o.orderDetails od
    JOIN od.product p
    WHERE o.createAt >= :startDate
    GROUP BY CAST(o.createAt AS date)
    ORDER BY CAST(o.createAt AS date)
""")
    List<DailyRevenue> getAdminRevenueFromDate(LocalDateTime startDate);
    @Query("""
    SELECT o.status, COUNT(DISTINCT o.orderId)
    FROM Order o
    GROUP BY o.status
""")
    List<Object[]> getAdminOrderStatusRaw();
    
    // Get order total for payment distribution
    @Query("SELECT o.total FROM Order o WHERE o.orderId = :orderId")
    java.math.BigDecimal findTotalByOrderId(UUID orderId);
    
    // Dashboard statistics methods
    
    /**
     * Get revenue breakdown by source (product, template, commission)
     * Requirements: 7.1, 7.2, 7.3, 7.4
     */
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN od.product IS NOT NULL 
                                 THEN od.unitPrice * od.quantity ELSE 0 END), 0) as productRevenue,
               COALESCE(SUM(CASE WHEN otd.template IS NOT NULL 
                                 THEN otd.unitPrice * otd.quantity ELSE 0 END), 0) as templateRevenue,
               0 as customRevenue,
               COALESCE(SUM(o.total * :commissionRate / 100), 0) as totalCommission
        FROM Order o
        LEFT JOIN OrderDetail od ON od.order = o
        LEFT JOIN OrderTemplateDetail otd ON otd.order = o
        WHERE o.createAt >= :startDate
        """)
    RevenueBreakdown getRevenueBreakdown(
        @Param("startDate") LocalDateTime startDate, 
        @Param("commissionRate") BigDecimal commissionRate
    );
    
    // ==================== Artisan Dashboard Statistics Methods ====================
    
    /**
     * Get order fulfillment rate for an artisan
     * Calculates percentage of delivered orders vs total non-cancelled orders
     * Requirements: 3.4, 7.7
     */
    @Query("SELECT " +
           "(CAST(COUNT(CASE WHEN o.status = 'DELIVERED' THEN 1 END) AS DOUBLE) * 100.0 / " +
           "NULLIF(COUNT(CASE WHEN o.status != 'CANCELLED' THEN 1 END), 0)) as fulfillmentRate " +
           "FROM Order o " +
           "LEFT JOIN o.orderDetails od " +
           "LEFT JOIN o.templateDetails otd " +
           "WHERE od.product.artisan.artisanUuid = :artisanId OR otd.template.artisan.artisanUuid = :artisanId")
    Double getOrderFulfillmentRate(@Param("artisanId") UUID artisanId);
    
    /**
     * Get on-time delivery rate for an artisan
     * Calculates percentage of orders delivered on or before expected date
     * Requirements: 3.6, 7.7
     */
    @Query("SELECT " +
           "(CAST(COUNT(CASE WHEN s.actualDelivery IS NOT NULL AND s.actualDelivery <= s.estimatedDelivery THEN 1 END) AS DOUBLE) * 100.0 / " +
           "NULLIF(COUNT(CASE WHEN o.status = 'DELIVERED' AND s.actualDelivery IS NOT NULL THEN 1 END), 0)) as onTimeRate " +
           "FROM Order o " +
           "LEFT JOIN o.orderDetails od " +
           "LEFT JOIN o.templateDetails otd " +
           "LEFT JOIN Shipment s ON s.order = o " +
           "WHERE (od.product.artisan.artisanUuid = :artisanId OR otd.template.artisan.artisanUuid = :artisanId) " +
           "AND o.status = 'DELIVERED'")
    Double getOnTimeDeliveryRate(@Param("artisanId") UUID artisanId);
}
