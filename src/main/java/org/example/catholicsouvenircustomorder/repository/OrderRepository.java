package org.example.catholicsouvenircustomorder.repository;


import org.example.catholicsouvenircustomorder.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByCustomerAccountId(UUID customerId);
}
