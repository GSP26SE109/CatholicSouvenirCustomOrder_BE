package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.OrderGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderGroupRepository extends JpaRepository<OrderGroup, UUID> {
    
    List<OrderGroup> findByCustomer_AccountId(UUID customerId);
    
    List<OrderGroup> findByCustomer_AccountIdOrderByCreatedAtDesc(UUID customerId);
}
