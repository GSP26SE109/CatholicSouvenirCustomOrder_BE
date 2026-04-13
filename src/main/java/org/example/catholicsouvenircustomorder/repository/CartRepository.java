package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {
    
    Optional<Cart> findByCustomer_AccountId(UUID customerId);
    
    boolean existsByCustomer_AccountId(UUID customerId);
}
