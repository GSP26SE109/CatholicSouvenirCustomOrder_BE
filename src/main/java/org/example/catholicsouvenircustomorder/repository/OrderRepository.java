package org.example.catholicsouvenircustomorder.repository;


import org.example.catholicsouvenircustomorder.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByCustomerAccountId(UUID customerId, Pageable pageable);
    
    List<Order> findByCustomer_AccountId(UUID customerId);
    
    @Query("""
    SELECT DISTINCT o
    FROM Order o
    JOIN o.orderDetails od
    JOIN od.product p
    WHERE p.artisan.artisanUuid = :artisanId
""")
    Page<Order> findOrdersByArtisanId(UUID artisanId,Pageable pageable);
}
