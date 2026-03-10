package org.example.catholicsouvenircustomorder.repository;


import org.example.catholicsouvenircustomorder.dto.response.Dashboard.DailyRevenue;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.DashboardSummary;
import org.example.catholicsouvenircustomorder.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    SELECT COUNT(o) as totalOrders,
    SUM(o.total) as totalRevenue
    FROM Order o
    WHERE o.createAt >= :start
""")
    DashboardSummary getSummary(LocalDateTime start);

    @Query("""
    SELECT DATE(o.createAt) AS day,
           SUM(o.total) AS revenue
    FROM Order o
    WHERE o.createAt >= :startDate
    GROUP BY DATE(o.createAt)
    ORDER BY DATE(o.createAt)
""")
    DailyRevenue getRevenueFromDate(@Param("startDate") LocalDateTime startDate);

    @Query("""
    SELECT o.status, COUNT(*) as total
    FROM Order o
    GROUP BY o.status
""")
    List<Object[]> getOrderStatusRaw();
}
