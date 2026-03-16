package org.example.catholicsouvenircustomorder.repository;


import org.example.catholicsouvenircustomorder.dto.response.Dashboard.DailyRevenue;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.DashboardSummary;
import org.example.catholicsouvenircustomorder.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByCustomerAccountId(UUID customerId);

    @Query("""
    SELECT DISTINCT o
    FROM Order o
    JOIN o.orderDetails od
    JOIN od.product p
    WHERE p.artisan.artisanUuid = :artisanId
""")
    List<Order> findOrdersByArtisanId(UUID artisanId);

    @Query("""
    SELECT COUNT(DISTINCT o.orderId) AS totalOrders,
           SUM(od.quantity * p.productPrice) AS totalRevenue
    FROM Order o
    JOIN o.orderDetails od
    JOIN od.product p
    WHERE o.createAt >= :start
    AND p.artisan.artisanUuid = :artisanId
""")
    DashboardSummary getSummary(LocalDateTime start, UUID artisanId);

    @Query("""
    SELECT CAST(o.createAt AS date) AS date,
           COUNT(o.orderId) AS orderNumber,
           SUM(od.quantity * p.productPrice) AS revenue
    FROM Order o
    JOIN o.orderDetails od
    JOIN od.product p
    WHERE o.createAt >= :startDate
    AND p.artisan.artisanUuid = :artisanId
    GROUP BY CAST(o.createAt AS date)
    ORDER BY CAST(o.createAt AS date)
""")
    List<DailyRevenue> getRevenueFromDate(LocalDateTime startDate, UUID artisanId);

    @Query("""
    SELECT o.status, COUNT(DISTINCT o.orderId)
    FROM Order o
    JOIN o.orderDetails od
    JOIN od.product p
    WHERE p.artisan.artisanUuid = :artisanId
    GROUP BY o.status
""")
    List<Object[]> getOrderStatusRaw(UUID artisanId);
}
