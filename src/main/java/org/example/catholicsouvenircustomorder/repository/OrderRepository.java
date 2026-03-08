package org.example.catholicsouvenircustomorder.repository;


import org.example.catholicsouvenircustomorder.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
